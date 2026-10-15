package patchlib.agent.matchers;

import net.bytebuddy.description.annotation.AnnotationDescription;
import net.bytebuddy.description.field.FieldDescription;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.matcher.ElementMatcher;
import patchlib.api.spec.AnnotationQuerySpec;
import patchlib.api.spec.ClassQuerySpec;
import patchlib.api.spec.FieldQuerySpec;


import static net.bytebuddy.matcher.ElementMatchers.*;

public class FieldMatcher {

    public static ElementMatcher.Junction<FieldDescription> fromQuery(FieldQuerySpec query) {
        return fromQuery(query, null);
    }

    public static ElementMatcher.Junction<FieldDescription> fromQuery(FieldQuerySpec query, ClassQuerySpec ownerQuery) {
        ElementMatcher.Junction<FieldDescription> matcher = any();

        if (!query.fieldName().isEmpty()) {
            matcher = matcher.and(named(query.fieldName()));
        }

        if (!query.typeName().isEmpty()) {
            matcher = matcher.and(fieldType(named(query.typeName())));
        }

        if (!query.subtypeName().isEmpty()) {
            matcher = matcher.and(failSafe(fieldType(hasSuperType(named(query.subtypeName())))));
        }

        switch (query.staticType()) {
            case STATIC -> matcher = matcher.and(isStatic());
            case INSTANCE -> matcher = matcher.and(not(isStatic()));
            case ANY -> { }
        }

        for (AnnotationQuerySpec annotation : query.annotations()) {
            ElementMatcher.Junction<AnnotationDescription> annotationMatcher = AnnotationMatcher.fromQuery(annotation);
            matcher = matcher.and(declaresAnnotation(annotationMatcher));
        }

        //Check if the field is part of some class, used only by redirects.
        if (ownerQuery != null) {
            ElementMatcher.Junction<TypeDescription> ownerMatcher = ClassMatcher.fromQuery(ownerQuery);
            matcher = matcher.and(isDeclaredBy(ownerMatcher));
        }

        return matcher;
    }

}
