package patchlib.agent.discover;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.ModSpecAPI;
import net.bytebuddy.description.annotation.AnnotationDescription;
import net.bytebuddy.description.annotation.AnnotationList;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.ClassFileLocator;
import net.bytebuddy.pool.TypePool;
import patchlib.agent.JarClasses;
import patchlib.agent.PatchLibLogger;
import patchlib.agent.spec.*;
import patchlib.api.match.MethodType;
import patchlib.api.match.Unset;
import patchlib.api.patch.After;
import patchlib.api.patch.Before;
import patchlib.api.patch.Except;
import patchlib.api.patch.Patch;
import patchlib.api.patch.RedirectCall;
import patchlib.api.patch.RedirectFieldRead;
import patchlib.api.patch.RedirectFieldWrite;
import patchlib.api.patch.RedirectNew;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.jar.JarFile;

/** Scans all loaded mod jars for patch annotations and creates the patch specs */
public class PatchScanner {

    static final String PATCH = Patch.class.getName();
    static final String BEFORE = Before.class.getName();
    static final String AFTER = After.class.getName();
    static final String EXCEPT = Except.class.getName();
    static final String REDIRECT_CALL = RedirectCall.class.getName();
    static final String REDIRECT_NEW = RedirectNew.class.getName();
    static final String REDIRECT_FIELD_READ = RedirectFieldRead.class.getName();
    static final String REDIRECT_FIELD_WRITE = RedirectFieldWrite.class.getName();
    static final String UNSET = Unset.class.getName();

    /** Every patch annotation a handler method can carry. When several are present, the first in this order wins. */
    private static final List<String> METHOD_ANNOTATIONS = List.of(
            BEFORE, AFTER, EXCEPT, REDIRECT_CALL, REDIRECT_NEW, REDIRECT_FIELD_READ, REDIRECT_FIELD_WRITE);

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
        PatchLibLogger.info("Finished grabbing jars");

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
        try (ClassFileLocator locator = new ClassFileLocator.Compound(locators)) {

            //Type pool for grabbing information from classes. Set to "FAST" so that method bodies are skipped in parsing.
            TypePool pool = new TypePool.Default(new TypePool.CacheProvider.Simple(), locator, TypePool.Default.ReaderMode.FAST);

            //Check every class of every mods jars
            for (JarPair jarPair : jarPairs) {
                try (JarFile jarFile = new JarFile(jarPair.jar)) {
                    for (String binaryName : JarClasses.namesIn(jarFile)) {
                        try {
                            scanClass(pool.describe(binaryName).resolve(), jarPair.mod, patches);
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

    /** Collects the patch specs of one class's handler methods. Classes without @Patch are skipped. */
    private void scanClass(TypeDescription type, ModSpecAPI mod, List<PatchSpec> patches) {
        AnnotationDescription patchAnnotation = getAnnotation(type.getDeclaredAnnotations(), PATCH);
        if (patchAnnotation == null) return;

        TargetClassSpec classSpec = createClassSpec(AnnotationReader.readAnnotation(patchAnnotation, "target"));

        for (MethodDescription.InDefinedShape handlerMethod : type.getDeclaredMethods()) {
            AnnotationDescription annotation = findPatchAnnotation(type.getName(), handlerMethod);
            if (annotation == null) continue;
            patches.add(createSpec(annotation, mod, type.getName(), handlerMethod.getName(), classSpec));
        }
    }

    /** The patch annotation on a handler method, or null if it has none.
     * Only one patch annotation is allowed per method, any past the first are ignored. */
    private AnnotationDescription findPatchAnnotation(String className, MethodDescription.InDefinedShape method) {
        List<AnnotationDescription> found = new ArrayList<>();
        for (String annotationName : METHOD_ANNOTATIONS) {
            AnnotationDescription annotation = getAnnotation(method.getDeclaredAnnotations(), annotationName);
            if (annotation != null) found.add(annotation);
        }

        if (found.isEmpty()) return null;
        if (found.size() > 1) {
            PatchLibLogger.warn("Multiple patch annotations on " + className + "#" + method.getName()
                    + ", only the first patch annotation is used");
        }
        return found.get(0);
    }

    /** Builds the spec for one handler method from its patch annotation. */
    private PatchSpec createSpec(AnnotationDescription annotation, ModSpecAPI mod, String handlerClass,
                                 String handlerMethod, TargetClassSpec classSpec) {
        String name = annotation.getAnnotationType().getName();

        if (name.equals(BEFORE)) return adviceSpec(PatchType.BEFORE, annotation, mod, handlerClass, handlerMethod, classSpec);
        if (name.equals(AFTER)) return adviceSpec(PatchType.AFTER, annotation, mod, handlerClass, handlerMethod, classSpec);
        if (name.equals(EXCEPT)) return adviceSpec(PatchType.EXCEPT, annotation, mod, handlerClass, handlerMethod, classSpec);

        if (name.equals(REDIRECT_CALL)) return redirectSpec(createCallSiteSpec(annotation), annotation, mod, handlerClass, handlerMethod, classSpec);
        if (name.equals(REDIRECT_NEW)) return redirectSpec(createNewSiteSpec(annotation), annotation, mod, handlerClass, handlerMethod, classSpec);
        if (name.equals(REDIRECT_FIELD_READ)) return redirectSpec(createFieldSiteSpec(annotation, RedirectKind.FIELD_READ), annotation, mod, handlerClass, handlerMethod, classSpec);
        if (name.equals(REDIRECT_FIELD_WRITE)) return redirectSpec(createFieldSiteSpec(annotation, RedirectKind.FIELD_WRITE), annotation, mod, handlerClass, handlerMethod, classSpec);

        throw new IllegalStateException("Unhandled patch annotation " + name);
    }

    private PatchSpec adviceSpec(PatchType patchType, AnnotationDescription annotation, ModSpecAPI mod,
                                 String handlerClass, String handlerMethod, TargetClassSpec classSpec) {
        PatchLibLogger.info("Discovered Patch  -  Class: " + handlerClass + "; Handler Method: " + handlerMethod + ";");
        return new PatchSpec(mod, handlerClass, handlerMethod, patchType,
                AnnotationReader.readInt(annotation, "priority", 0),
                classSpec,
                createMethodSpec(AnnotationReader.readAnnotation(annotation, "target")),
                null);
    }

    private PatchSpec redirectSpec(RedirectSiteSpec siteSpec, AnnotationDescription annotation, ModSpecAPI mod,
                                   String handlerClass, String handlerMethod, TargetClassSpec classSpec) {
        PatchLibLogger.info("Discovered Redirect  -  Class: " + handlerClass + "; Handler Method: " + handlerMethod + ";");
        return new PatchSpec(mod, handlerClass, handlerMethod, PatchType.REDIRECT,
                AnnotationReader.readInt(annotation, "priority", 0),
                classSpec,
                createMethodSpec(AnnotationReader.readAnnotation(annotation, "target")),
                siteSpec);
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

        String[] attachedAnnotations = annotationFilter(annotation);

        return new TargetClassSpec(
                !type.isEmpty() ? type : typeName,
                !subtype.isEmpty() ? subtype : subtypeName,
                targetPackage,
                includeSubpackages,
                excludePackage,
                excludeSubpackages,
                methodMatches,
                fieldMatches,
                attachedAnnotations
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

        String methodTypeName = AnnotationReader.readEnumName(annotation, "methodType", "ANY");
        MethodType methodType = MethodType.valueOf(methodTypeName);

        boolean staticOnly = AnnotationReader.readBoolean(annotation, "staticOnly", false);

        String[] attachedAnnotations = annotationFilter(annotation);

        return new TargetMethodSpec(
                methodName,
                parameters.length != 0 ? parameters : parameterNames,
                parameterCount,
                !returnType.isEmpty() ? returnType : returnTypeName,
                methodType,
                staticOnly,
                attachedAnnotations
                );
    }

    /** The annotation-presence filter of a @ClassMatch or @MethodMatch, as erased annotation type names.
     * The typed annotations() member wins over the annotationNames() name variant when both are set. */
    private String[] annotationFilter(AnnotationDescription annotation) {
        String[] annotations = AnnotationReader.readTypeArray(annotation, "annotations");
        return annotations.length != 0 ? annotations : AnnotationReader.readStringArray(annotation, "annotationNames");
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

    /** Builds the construction site spec from a @RedirectNew. The constructor shape reuses @MethodMatch; only its
     * parameter members are used. The declaring type is the instantiated class. */
    private RedirectSiteSpec createNewSiteSpec(AnnotationDescription redirectAnnotation) {
        AnnotationDescription constructor = AnnotationReader.readAnnotation(redirectAnnotation, "constructor");
        String[] parameters = AnnotationReader.readTypeArray(constructor, "parameters");
        String[] parameterNames = AnnotationReader.readStringArray(constructor, "parameterNames");

        return new RedirectSiteSpec(
                RedirectKind.CONSTRUCTOR,
                createClassSpec(AnnotationReader.readAnnotation(redirectAnnotation, "type")),
                "", //name is unused, a constructor has no name
                parameters.length != 0 ? parameters : parameterNames,
                AnnotationReader.readInt(constructor, "parameterCount", -1),
                "", //returnOrFieldType is unused, the result is the instantiated class
                "", //fieldSubtype is unused for constructor calls
                false);
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
