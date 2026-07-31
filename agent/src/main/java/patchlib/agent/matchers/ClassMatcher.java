package patchlib.agent.matchers;

import net.bytebuddy.description.annotation.AnnotationDescription;
import net.bytebuddy.description.field.FieldDescription;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.type.PackageDescription;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.matcher.ElementMatcher;
import patchlib.api.spec.AnnotationQuerySpec;
import patchlib.api.spec.ClassQuerySpec;
import patchlib.api.spec.FieldQuerySpec;
import patchlib.api.spec.MethodQuerySpec;

import static net.bytebuddy.matcher.ElementMatchers.*;

public class ClassMatcher {

    public static ElementMatcher.Junction<TypeDescription> fromQuery(ClassQuerySpec query) {
        ElementMatcher.Junction<TypeDescription> matcher = any();

        if (!query.className().isEmpty()) {
            matcher = matcher.and(named(query.className()));
        }

        if (!query.packageName().isEmpty()) {
            matcher = matcher.and(description -> isInPackage(query, description));
        }

        if (!query.excludedPackageName().isEmpty()) {
            matcher = matcher.and(description -> isNotInPackage(query, description));
        }

        if (!query.subtypeName().isEmpty()) {
            matcher = matcher.and(failSafe(hasSuperType(named(query.subtypeName()))));
        }

        for (MethodQuerySpec method : query.methods()) {
            ElementMatcher.Junction<MethodDescription> methodMatcher = MethodMatcher.fromQuery(method);
            matcher = matcher.and(declaresMethod(methodMatcher));
        }

        for (FieldQuerySpec field : query.fields()) {
            ElementMatcher.Junction<FieldDescription> fieldMatcher = FieldMatcher.fromQuery(field);
            matcher = matcher.and(declaresField(fieldMatcher));
        }

        for (AnnotationQuerySpec annotation : query.annotations()) {
            ElementMatcher.Junction<AnnotationDescription> annotationMatcher = AnnotationMatcher.fromQuery(annotation);
            matcher = matcher.and(declaresAnnotation(annotationMatcher));
        }

        return matcher;
    }

    private static boolean isInPackage(ClassQuerySpec spec, TypeDescription description) {
        boolean includeSubpackages = spec.includeSubpackages();
        String packageName = spec.packageName();
        PackageDescription typePackage = description.getPackage();
        if (typePackage == null) return false;
        String typePackageName = typePackage.getActualName();

        if (includeSubpackages) {
            return typePackageName.equals(packageName) || typePackageName.startsWith(packageName + ".");
        } else {
            return typePackageName.equals(packageName);
        }
    }

    private static boolean isNotInPackage(ClassQuerySpec spec, TypeDescription description) {
        boolean excludeSubpackages = spec.excludeSubpackages();
        String packageName = spec.excludedPackageName();
        PackageDescription typePackage = description.getPackage();
        if (typePackage == null) return true;
        String typePackageName = typePackage.getActualName();

        if (excludeSubpackages) {
            return !(typePackageName.equals(packageName) || typePackageName.startsWith(packageName + "."));
        } else {
            return !typePackageName.equals(packageName);
        }
    }

}
