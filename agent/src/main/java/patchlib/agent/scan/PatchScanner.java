package patchlib.agent.scan;

import patchlib.agent.log.PatchLibLogger;
import patchlib.agent.spec.*;
import patchlib.api.PatchLib;
import patchlib.api.data.AnnotationData;
import patchlib.api.data.ClassData;
import patchlib.api.data.MethodData;
import patchlib.api.match.MethodType;
import patchlib.api.match.Unset;
import patchlib.api.patch.*;
import patchlib.api.query.AnnotationQuery;
import patchlib.api.query.ClassQuery;
import patchlib.api.query.FieldQuery;
import patchlib.api.query.MethodQuery;
import patchlib.api.spec.ClassQuerySpec;
import patchlib.api.spec.FieldQuerySpec;
import patchlib.api.spec.MethodQuerySpec;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class PatchScanner {

    private final static String BEFORE = Before.class.getTypeName();
    private final static String AFTER = After.class.getTypeName();
    private final static String EXCEPT = Except.class.getTypeName();
    private final static String REDIRECT_CALL = RedirectCall.class.getTypeName();
    private final static String REDIRECT_NEW = RedirectNew.class.getTypeName();
    private final static String REDIRECT_FIELD_READ = RedirectFieldRead.class.getTypeName();
    private final static String REDIRECT_FIELD_WRITE = RedirectFieldWrite.class.getTypeName();

    private static Set<String> PATCH_TYPES = new HashSet<>(Set.of(BEFORE, AFTER, EXCEPT,
            REDIRECT_CALL, REDIRECT_NEW, REDIRECT_FIELD_READ, REDIRECT_FIELD_WRITE));

    public List<PatchHandlerSpec> scan() {
        PatchLibLogger.info("Starting patch discovery");

        ClassQuerySpec query = ClassQuery.create()
                .hasAnnotation(AnnotationQuery.create()
                        .annotation(Patch.class))
                .build();

        //No point in checking game classes
        List<ClassData> classDataList = PatchLib.scan(query, true, false);

        List<PatchHandlerSpec> specs = createSpecs(classDataList);
        PatchLibLogger.info("Discovered " + specs.size() + " patch handlers");
        PatchLibLogger.info("Finished patch discovery");
        PatchLibLogger.blank();
        return specs;
    }

    private List<PatchHandlerSpec> createSpecs(List<ClassData> classDataList) {
        List<PatchHandlerSpec> specs = new ArrayList<>();
        for (ClassData classData : classDataList) {
            for (MethodData methodData: classData.getMethods()) {
                try {
                    PatchHandlerSpec spec = createSpec(classData, methodData);
                    if (spec == null) continue;
                    specs.add(spec);
                    PatchLibLogger.info("Discovered patch " + spec.handlerMethodName() + " in " + spec.handlerClassName() + "   (" + spec.sourceMod().getName() + ")");
                } catch (Exception ex) {
                    PatchLibLogger.error("Failed to parse the spec for method " + methodData.getName() + " from" + classData.getName() +
                            " (" + classData.getSourceMod().getName() + ")", ex);
                }
            }
        }
        return specs;
    }

    private PatchHandlerSpec createSpec(ClassData classData, MethodData methodData) {
        String className = classData.getName();

        AnnotationData patchAnnotation = classData.getAnnotation(Patch.class.getTypeName());

        AnnotationData patchTypeAnnotation = getFirstPatchAnnotation(methodData);
        if (patchTypeAnnotation == null) return null;
        PatchSpec patchSpec = createPatchSpec(patchTypeAnnotation);
        if (patchSpec == null) return null;
        int priority = patchTypeAnnotation.getInt("priority");

        ClassQuerySpec targetClass = createClassQuery(patchAnnotation.getAnnotation("target")).build();
        MethodQuerySpec targetMethod = createMethodQuery(patchTypeAnnotation.getAnnotation("target")).build();

        return new PatchHandlerSpec(className, methodData.getName(), classData.getSourceMod(), priority, targetClass, targetMethod, patchSpec);
    }

    private PatchSpec createPatchSpec(AnnotationData patchType) {
        String name = patchType.getName();

        if (name.equals(BEFORE) || name.equals(AFTER) || name.equals(EXCEPT)) {
            return new AdviceSpec(getAdviceType(patchType));
        }
        else if (name.equals(REDIRECT_CALL)) {
            ClassQuerySpec owner = createClassQuery(patchType.getAnnotation("owner")).build();
            MethodQuerySpec call = createMethodQuery(patchType.getAnnotation("call"))
                    .methodType(MethodType.METHOD) //Only target methods
                    .build();
            return new RedirectCallSpec(owner, call);
        }
        else if (name.equals(REDIRECT_NEW)) {
            ClassQuerySpec type = createClassQuery(patchType.getAnnotation("type")).build();
            MethodQuerySpec constructor = createMethodQuery(patchType.getAnnotation("constructor"))
                    .methodType(MethodType.CONSTRUCTOR) //Only target constructors
                    .returnTypeName("") //Constructors have no return type
                    .methodName("") //Constructors have no name
                    .staticOnly(false) //Constructors are never static
                    .build();
            return new RedirectNewSpec(type, constructor);
        }
        else if (name.equals(REDIRECT_FIELD_READ)) {
            ClassQuerySpec owner = createClassQuery(patchType.getAnnotation("owner")).build();
            FieldQuerySpec field = createFieldQuery(patchType.getAnnotation("field")).build();
            return new RedirectFieldReadSpec(owner, field);
        }
        else if (name.equals(REDIRECT_FIELD_WRITE)) {
            ClassQuerySpec owner = createClassQuery(patchType.getAnnotation("owner")).build();
            FieldQuerySpec field = createFieldQuery(patchType.getAnnotation("field")).build();
            return new RedirectFieldWriteSpec(owner, field);
        }

        return null;
    }

    private AdviceSpec.AdviceType getAdviceType(AnnotationData patchType) {
        String name = patchType.getName();
        if (name.equals(BEFORE)) return AdviceSpec.AdviceType.BEFORE;
        else if (name.equals(AFTER)) return AdviceSpec.AdviceType.AFTER;
        else return AdviceSpec.AdviceType.EXCEPT;
    }

    private AnnotationData getFirstPatchAnnotation(MethodData methodData) {
        for (AnnotationData annotationData : methodData.getAnnotations()) {
            if (PATCH_TYPES.contains(annotationData.getName())) {
                return annotationData;
            }
        }
        return null;
    }

    private ClassQuery createClassQuery(AnnotationData classMatch) {
        ClassQuery query = ClassQuery.create()
                .className(classOrString(classMatch.getClassName("type"), classMatch.getString("typeName")))
                .subtypeName(classOrString(classMatch.getClassName("subtype"), classMatch.getString("subtypeName")))
                .packageName(classMatch.getString("targetPackage"))
                .includeSubpackages(classMatch.getBoolean("includeSubpackages"))
                .excludedPackageName(classMatch.getString("excludePackage"))
                .excludeSubpackages(classMatch.getBoolean("excludeSubpackages"));

        for (AnnotationData methodMatch : classMatch.getAnnotationArray("methodMatches")) {
            query.hasMethod(createMethodQuery(methodMatch));
        }

        for (AnnotationData fieldMatches : classMatch.getAnnotationArray("fieldMatches")) {
            query.hasField(createFieldQuery(fieldMatches));
        }

        for (String annotationTypeName : classOrStringArray(classMatch.getClassNameArray("annotations"), classMatch.getStringArray("annotationNames"))) {
            query.hasAnnotation(AnnotationQuery.create().annotationName(annotationTypeName));
        }

        return query;
    }

    private MethodQuery createMethodQuery(AnnotationData methodMatch) {
        MethodQuery query = MethodQuery.create()
                .methodName(methodMatch.getString("methodName"))
                .parameterNames(classOrStringArray(methodMatch.getClassNameArray("parameters"), methodMatch.getStringArray("parameterNames")))
                .parameterCount(methodMatch.getInt("parameterCount"))
                .returnTypeName(classOrString(methodMatch.getClassName("returnType"), methodMatch.getString("returnTypeName")))
                .methodType(MethodType.valueOf(methodMatch.getEnumValue("methodType")))
                .staticOnly(methodMatch.getBoolean("staticOnly"));

        for (String annotationTypeName : classOrStringArray(methodMatch.getClassNameArray("annotations"), methodMatch.getStringArray("annotationNames"))) {
            query.hasAnnotation(AnnotationQuery.create().annotationName(annotationTypeName));
        }

        return query;
    }

    private FieldQuery createFieldQuery(AnnotationData fieldMatch) {
        FieldQuery query = FieldQuery.create()
                .fieldName(fieldMatch.getString("fieldName"))
                .fieldTypeName(classOrString(fieldMatch.getClassName("type"), fieldMatch.getString("typeName")))
                .fieldSubtypeName(classOrString(fieldMatch.getClassName("subtype"), fieldMatch.getString("subtypeName")))
                .staticOnly(fieldMatch.getBoolean("staticOnly"));

        return query;
    }

    private String[] classOrStringArray(String[] classes, String[] strings) {
        if (classes.length != 0) return classes;
        return strings;
    }

    private String classOrString(String clazz, String string) {
        if (!clazz.equals(Unset.class.getTypeName())) return clazz;
        return string;
    }

}
