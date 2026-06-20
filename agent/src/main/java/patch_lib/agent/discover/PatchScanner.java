package patch_lib.agent.discover;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.ModSpecAPI;
import net.bytebuddy.description.annotation.AnnotationDescription;
import net.bytebuddy.description.annotation.AnnotationList;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.ClassFileLocator;
import net.bytebuddy.pool.TypePool;
import patch_lib.agent.PatchLibLogger;
import patch_lib.agent.spec.*;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/** Scans all loaded mod jars for patch annotations and creates the patch specs */
public class PatchScanner {

    static final String PATCH = "patch_lib.api.patch.Patch";
    static final String BEFORE = "patch_lib.api.patch.Before";
    static final String AFTER = "patch_lib.api.patch.After";
    static final String UNSET = "patch_lib.api.match.Unset";

    record JarPair(ModSpecAPI mod, File jar) { }

    public List<PatchSpec> scan() {

        //Collect the jars of every enabled mod
        List<ModSpecAPI> enabledMods = Global.getSettings().getModManager().getEnabledModsCopy();
        List<JarPair> jarPairs = enabledMods.stream()
                .flatMap( spec ->
                        spec.getJars().stream()
                                .map( jar -> new JarPair(spec, new File(spec.getPath(), jar))) )
                .toList();

        PatchLibLogger.info("Starting annotation scan in the following jars: ");
        jarPairs.forEach(jar -> PatchLibLogger.info(" - " + jar.jar.getPath()));
        PatchLibLogger.info("Finished grabbing jars ");

        //Create the class file locators for scanning the bytes of the classes
        List<ClassFileLocator> locators = new ArrayList<>();
        locators.add(ClassFileLocator.ForClassLoader.ofSystemLoader()); //Required to read JVM and game Classes that appear on the annotations
        try {
            for (JarPair jarPair : jarPairs) {
                locators.add(ClassFileLocator.ForJarFile.of(jarPair.jar));
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        PatchLibLogger.info("Starting patch search");
        List<PatchSpec> patches = new ArrayList<>();
        try (ClassFileLocator locator = new ClassFileLocator.Compound(locators);) {

            //Type pool for grabbing information from classes. Set to "FAST" so that method bodies are skipped in parsing.
            TypePool pool = new TypePool.Default(new TypePool.CacheProvider.Simple(), locator, TypePool.Default.ReaderMode.FAST);

            //Iterate through all mods jars
            for (JarPair jarPair : jarPairs) {
                try (JarFile jarFile = new JarFile(jarPair.jar)) {
                    Enumeration<JarEntry> entries = jarFile.entries();

                    //Iterate over every class in the jar
                    while (entries.hasMoreElements()) {
                        JarEntry entry = entries.nextElement();

                        //Skip non-classes
                        if (entry.isDirectory() || !entry.getName().endsWith(".class")) continue;

                        String name = entry.getName();

                        //Create actual full class path in package format.
                        String binaryName = name.substring(0, name.length() - ".class".length())
                                .replace('/', '.');

                        try {
                            TypeDescription type = pool.describe(binaryName).resolve();

                            AnnotationDescription patchAnnotation = getAnnotation(type.getDeclaredAnnotations(), PATCH);
                            //Ignore all classes that aren't patches
                            if (patchAnnotation == null) continue;

                            TargetClassSpec classSpec = createClassSpec(patchAnnotation);

                            for (MethodDescription.InDefinedShape handledMethod : type.getDeclaredMethods()) {

                                //Check for the annotation.
                                //Only one patch annotation is allowed per method, any past the first are ignored.
                                AnnotationDescription methodAnnotation = getAnnotation(handledMethod.getDeclaredAnnotations(), BEFORE);
                                if (methodAnnotation == null) {
                                    methodAnnotation = getAnnotation(handledMethod.getDeclaredAnnotations(), AFTER);
                                }
                                if (methodAnnotation == null) continue; //Skip if neither exist

                                TargetMethodSpec methodSpec = createMethodSpec(methodAnnotation);
                                int priority = AnnotationReader.readInt(methodAnnotation, "priority", 0);
                                String methodAnnotationName = methodAnnotation.getAnnotationType().getName();

                                PatchType patchType = switch (methodAnnotationName) {
                                    case BEFORE -> PatchType.BEFORE;
                                    case AFTER -> PatchType.AFTER;
                                    default -> PatchType.BEFORE;
                                };

                                PatchSpec patchSpec = new PatchSpec(
                                        jarPair.mod,
                                        binaryName,
                                        handledMethod.getName(),
                                        patchType,
                                        priority,
                                        classSpec,
                                        methodSpec
                                );

                                patches.add(patchSpec);
                                PatchLibLogger.info("Discovered Patch  -  Class: " + binaryName + "; Handler Method: " + handledMethod.getName() + ";" );

                            }


                        } catch (Exception ex) {
                            PatchLibLogger.info("Failed to scan " + binaryName + ": " + ex);
                        }
                    }
                }
            }

        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
        PatchLibLogger.info("Finished patch search");
        PatchLibLogger.info("Discovered " + patches.size() + " patch");

        return patches;
    }

    private TargetClassSpec createClassSpec(AnnotationDescription annotation) {
        String targetClass = AnnotationReader.readType(annotation, "targetClass", "");
        String targetClassName = AnnotationReader.readString(annotation, "targetClassName", "");

        String targetSubtype = AnnotationReader.readType(annotation, "targetSubtype", "");
        String targetSubtypeName = AnnotationReader.readString(annotation, "targetSubtypeName", "");

        String targetPackage = AnnotationReader.readString(annotation, "targetPackage", "");
        boolean includeSubpackages = AnnotationReader.readBoolean(annotation, "includeSubpackages", false);

        //Matchers for searching classes by contained methods, not the patching annotations one.
        AnnotationDescription[] matchAnnotations = AnnotationReader.readAnnotationArray(annotation, "methodMatches");
        TargetMethodSpec[] methodMatches = new TargetMethodSpec[matchAnnotations.length];
        for (int i = 0; i < matchAnnotations.length; i++) {
            methodMatches[i] = createMethodSpec(matchAnnotations[i]);
        }

        //Matchers for searching classes by contained fields.
        AnnotationDescription[] fieldMatchAnnotations = AnnotationReader.readAnnotationArray(annotation, "fieldMatches");
        TargetFieldSpec[] fieldMatches = new TargetFieldSpec[fieldMatchAnnotations.length];
        for (int i = 0; i < fieldMatchAnnotations.length; i++) {
            fieldMatches[i] = createFieldSpec(fieldMatchAnnotations[i]);
        }

        return new TargetClassSpec(
                !targetClass.isEmpty() ? targetClass : targetClassName,
                !targetSubtype.isEmpty() ? targetSubtype : targetSubtypeName,
                targetPackage,
                includeSubpackages,
                methodMatches,
                fieldMatches
        );
    }

    private TargetFieldSpec createFieldSpec(AnnotationDescription annotation) {
        String fieldName = AnnotationReader.readString(annotation, "fieldName", "");

        String fieldType = AnnotationReader.readType(annotation, "fieldType", "");
        String fieldTypeName = AnnotationReader.readString(annotation, "fieldTypeName", "");

        String fieldSubtype = AnnotationReader.readType(annotation, "fieldSubtype", "");
        String fieldSubtypeName = AnnotationReader.readString(annotation, "fieldSubtypeName", "");

        boolean staticOnly = AnnotationReader.readBoolean(annotation, "staticOnly", false);

        return new TargetFieldSpec(
                fieldName,
                !fieldType.isEmpty() ? fieldType : fieldTypeName,
                !fieldSubtype.isEmpty() ? fieldSubtype : fieldSubtypeName,
                staticOnly
        );
    }

    private TargetMethodSpec createMethodSpec(AnnotationDescription annotation) {
        String methodName = AnnotationReader.readString(annotation, "methodName", "");

        String[] parameters = AnnotationReader.readTypeArray(annotation, "parameters");
        String[] parameterNames = AnnotationReader.readStringArray(annotation, "parameterNames");
        int parameterCount = AnnotationReader.readInt(annotation, "parameterCount", -1);

        String returnType = AnnotationReader.readType(annotation, "returnType", "");
        String returnTypeName = AnnotationReader.readString(annotation, "returnTypeName", "");

        //Not needed for the method spec, grabbed by the PatchSpec instead.
        //int priority = AnnotationReader.readInt(annotation, "priority", 0);

        String methodTypeName = AnnotationReader.readEnumName(annotation, "methodType", "ANY");
        MethodType methodType = MethodType.valueOf(methodTypeName);

        boolean staticOnly = AnnotationReader.readBoolean(annotation, "staticOnly", false);

        return new TargetMethodSpec(
                methodName,
                parameters.length != 0 ? parameters : parameterNames,
                parameterCount,
                !returnType.isEmpty() ? returnType : returnTypeName,
                methodType,
                staticOnly
                );
    }



    private AnnotationDescription getAnnotation(AnnotationList annotations, String fullClassName) {
        for (AnnotationDescription annotation : annotations) {
            if (annotation.getAnnotationType().getName().equals(fullClassName)) return annotation;
        }
        return null;
    }

}
