package patchlib.agent.matchers;

import net.bytebuddy.description.annotation.AnnotationDescription;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.matcher.ElementMatcher;
import patchlib.api.spec.AnnotationQuerySpec;
import patchlib.api.spec.ClassQuerySpec;

import static net.bytebuddy.matcher.ElementMatchers.*;

public class AnnotationMatcher {

    public static ElementMatcher.Junction<AnnotationDescription> fromQuery(AnnotationQuerySpec query) {
        ElementMatcher.Junction<AnnotationDescription> matcher = any();

        if (!query.annotationName().isEmpty()) {
            matcher = matcher.and(annotationType(named(query.annotationName())));
        }

        return matcher;
    }

}
