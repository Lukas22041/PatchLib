package patchlib.api.query;

import patchlib.api.spec.AnnotationQuerySpec;

public class AnnotationQuery {

    private String annotationName = "";

    private AnnotationQuery() { }

    public static AnnotationQuery create() {
        return new AnnotationQuery();
    }

    public AnnotationQuerySpec build() {
        return new AnnotationQuerySpec(annotationName);
    }

    public AnnotationQuery annotation(Class<?> annotation) {
        this.annotationName = annotation.getTypeName();
        return this;
    }

    public AnnotationQuery annotationName(String annotationName) {
        this.annotationName = annotationName;
        return this;
    }

}
