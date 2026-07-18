package patchlib.api.scan;

public class AnnotationScanBuilder {

    private String annotationName = "";

    private AnnotationScanBuilder() { }

    public static AnnotationScanBuilder create() {
        return new AnnotationScanBuilder();
    }

    public AnnotationScanBuilder annotation(Class<?> annotation) {
        this.annotationName = annotation.getTypeName();
        return this;
    }

    public AnnotationScanBuilder annotationName(String annotationName) {
        this.annotationName = annotationName;
        return this;
    }

}
