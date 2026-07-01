package patchlib.agent.discover;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.ModSpecAPI;
import net.bytebuddy.description.annotation.AnnotationDescription;
import net.bytebuddy.description.annotation.AnnotationList;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.ClassFileLocator;
import net.bytebuddy.pool.TypePool;
import patchlib.agent.PatchLibLogger;
import patchlib.agent.spec.*;
import patchlib.api.match.MethodType;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/** Scans all loaded mod jars for patch annotations and creates the patch specs */
public class PatchScanner {

    static final String PATCH = "patchlib.api.patch.Patch";
    static final String BEFORE = "patchlib.api.patch.Before";
    static final String AFTER = "patchlib.api.patch.After";
    static final String EXCEPT = "patchlib.api.patch.Except";
    static final String REDIRECT_CALL = "patchlib.api.patch.RedirectCall";
    static final String REDIRECT_FIELD_READ = "patchlib.api.patch.RedirectFieldRead";
    static final String REDIRECT_FIELD_WRITE = "patchlib.api.patch.RedirectFieldWrite";
    static final String UNSET = "patchlib.api.match.Unset";

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
        locators.add(ClassFileLocator.ForClassLoader.of(PatchScanner.class.getClassLoader()));
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

                            TargetClassSpec classSpec = createClassSpec(AnnotationReader.readAnnotation(patchAnnotation, "target"));

                            for (MethodDescription.InDefinedShape handledMethod : type.getDeclaredMethods()) {

                                AnnotationDescription before = getAnnotation(handledMethod.getDeclaredAnnotations(), BEFORE);
                                AnnotationDescription after = getAnnotation(handledMethod.getDeclaredAnnotations(), AFTER);
                                AnnotationDescription except = getAnnotation(handledMethod.getDeclaredAnnotations(), EXCEPT);
                                AnnotationDescription redirectCall = getAnnotation(handledMethod.getDeclaredAnnotations(), REDIRECT_CALL);
                                AnnotationDescription redirectRead = getAnnotation(handledMethod.getDeclaredAnnotations(), REDIRECT_FIELD_READ);
                                AnnotationDescription redirectWrite = getAnnotation(handledMethod.getDeclaredAnnotations(), REDIRECT_FIELD_WRITE);

                                //Only one patch annotation is allowed per method, any past the first are ignored.
                                int annotationCount = (before != null ? 1 : 0) + (after != null ? 1 : 0) + (except != null ? 1 : 0)
                                        + (redirectCall != null ? 1 : 0) + (redirectRead != null ? 1 : 0) + (redirectWrite != null ? 1 : 0);
                                if (annotationCount == 0) continue;
                                if (annotationCount > 1) {
                                    PatchLibLogger.warn("Multiple patch annotations on " + binaryName + "#" + handledMethod.getName()
                                            + ", only the first patch annotation is used");
                                }

                                AnnotationDescription methodAnnotation = before != null ? before : after != null ? after : except;
                                if (methodAnnotation != null) {
                                    TargetMethodSpec methodSpec = createMethodSpec(AnnotationReader.readAnnotation(methodAnnotation, "target"));
                                    int priority = AnnotationReader.readInt(methodAnnotation, "priority", 0);
                                    String methodAnnotationName = methodAnnotation.getAnnotationType().getName();

                                    PatchType patchType = switch (methodAnnotationName) {
                                        case BEFORE -> PatchType.BEFORE;
                                        case AFTER -> PatchType.AFTER;
                                        case EXCEPT -> PatchType.EXCEPT;
                                        default -> PatchType.BEFORE;
                                    };

                                    patches.add(new PatchSpec(jarPair.mod, binaryName, handledMethod.getName(),
                                            patchType, priority, classSpec, methodSpec, null));
                                    PatchLibLogger.info("Discovered Patch  -  Class: " + binaryName + "; Handler Method: " + handledMethod.getName() + ";");
                                    continue;
                                }

                                AnnotationDescription redirectAnnotation = redirectCall != null ? redirectCall
                                        : redirectRead != null ? redirectRead : redirectWrite;
                                RedirectSiteSpec siteSpec = redirectCall != null
                                        ? createCallSiteSpec(redirectAnnotation)
                                        : createFieldSiteSpec(redirectAnnotation, redirectRead != null ? RedirectKind.FIELD_READ : RedirectKind.FIELD_WRITE);

                                TargetMethodSpec hostSpec = createMethodSpec(AnnotationReader.readAnnotation(redirectAnnotation, "target"));
                                int redirectPriority = AnnotationReader.readInt(redirectAnnotation, "priority", 0);

                                patches.add(new PatchSpec(jarPair.mod, binaryName, handledMethod.getName(),
                                        PatchType.REDIRECT, redirectPriority, classSpec, hostSpec, siteSpec));
                                PatchLibLogger.info("Discovered Redirect  -  Class: " + binaryName + "; Handler Method: " + handledMethod.getName() + ";");

                            }


                        } catch (Exception ex) {
                            PatchLibLogger.warn("Failed to scan " + binaryName + ": " + ex);
                        }
                    }
                }
            }

        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
        PatchLibLogger.info("Finished patch search");
        PatchLibLogger.info("Discovered " + patches.size() + " patches");

        return patches;
    }

    /** Builds a class spec from a @ClassMatch, used both for @Patch targets and for redirect owners. */
    private TargetClassSpec createClassSpec(AnnotationDescription annotation) {
        String type = AnnotationReader.readType(annotation, "type", "");
        String typeName = AnnotationReader.readString(annotation, "typeName", "");

        String subtype = AnnotationReader.readType(annotation, "subtype", "");
        String subtypeName = AnnotationReader.readString(annotation, "subtypeName", "");

        String targetPackage = AnnotationReader.readString(annotation, "targetPackage", "");
        boolean includeSubpackages = AnnotationReader.readBoolean(annotation, "includeSubpackages", false);

        String excludePackage = AnnotationReader.readString(annotation, "excludePackage", "");
        boolean excludeSubpackages = AnnotationReader.readBoolean(annotation, "excludeSubpackages", false);

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
                !type.isEmpty() ? type : typeName,
                !subtype.isEmpty() ? subtype : subtypeName,
                targetPackage,
                includeSubpackages,
                excludePackage,
                excludeSubpackages,
                methodMatches,
                fieldMatches
        );
    }

    private TargetFieldSpec createFieldSpec(AnnotationDescription annotation) {
        String fieldName = AnnotationReader.readString(annotation, "fieldName", "");

        String type = AnnotationReader.readType(annotation, "type", "");
        String typeName = AnnotationReader.readString(annotation, "typeName", "");

        String subtype = AnnotationReader.readType(annotation, "subtype", "");
        String subtypeName = AnnotationReader.readString(annotation, "subtypeName", "");

        boolean staticOnly = AnnotationReader.readBoolean(annotation, "staticOnly", false);

        return new TargetFieldSpec(
                fieldName,
                !type.isEmpty() ? type : typeName,
                !subtype.isEmpty() ? subtype : subtypeName,
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



    /** Builds the call site spec from a @RedirectCall. The call shape reuses @MethodMatch; its methodType is
     * ignored, an intercepted call is never a constructor. */
    private RedirectSiteSpec createCallSiteSpec(AnnotationDescription redirectAnnotation) {
        AnnotationDescription call = AnnotationReader.readAnnotation(redirectAnnotation, "call");
        String[] parameters = AnnotationReader.readTypeArray(call, "parameters");
        String[] parameterNames = AnnotationReader.readStringArray(call, "parameterNames");
        String returnType = AnnotationReader.readType(call, "returnType", "");
        String returnTypeName = AnnotationReader.readString(call, "returnTypeName", "");

        return new RedirectSiteSpec(
                RedirectKind.METHOD_CALL,
                ownerSpec(redirectAnnotation),
                AnnotationReader.readString(call, "methodName", ""),
                parameters.length != 0 ? parameters : parameterNames,
                AnnotationReader.readInt(call, "parameterCount", -1),
                !returnType.isEmpty() ? returnType : returnTypeName,
                "", //fieldSubtype is unused for method calls
                AnnotationReader.readBoolean(call, "staticOnly", false));
    }

    /** Builds the field access spec from a @RedirectFieldRead or @RedirectFieldWrite. The field shape reuses @FieldMatch. */
    private RedirectSiteSpec createFieldSiteSpec(AnnotationDescription redirectAnnotation, RedirectKind kind) {
        AnnotationDescription field = AnnotationReader.readAnnotation(redirectAnnotation, "field");
        String type = AnnotationReader.readType(field, "type", "");
        String typeName = AnnotationReader.readString(field, "typeName", "");
        String subtype = AnnotationReader.readType(field, "subtype", "");
        String subtypeName = AnnotationReader.readString(field, "subtypeName", "");

        return new RedirectSiteSpec(
                kind,
                ownerSpec(redirectAnnotation),
                AnnotationReader.readString(field, "fieldName", ""),
                new String[0],
                -1,
                !type.isEmpty() ? type : typeName,
                !subtype.isEmpty() ? subtype : subtypeName,
                AnnotationReader.readBoolean(field, "staticOnly", false));
    }

    /** The owner constraint of a redirect, from its @ClassMatch. An all-default owner matches everything. */
    private TargetClassSpec ownerSpec(AnnotationDescription redirectAnnotation) {
        return createClassSpec(AnnotationReader.readAnnotation(redirectAnnotation, "owner"));
    }

    private AnnotationDescription getAnnotation(AnnotationList annotations, String fullClassName) {
        for (AnnotationDescription annotation : annotations) {
            if (annotation.getAnnotationType().getName().equals(fullClassName)) return annotation;
        }
        return null;
    }

}
