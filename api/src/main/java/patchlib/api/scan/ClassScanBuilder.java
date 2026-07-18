package patchlib.api.scan;

import java.util.ArrayList;
import java.util.List;

public class ClassScanBuilder {

    private String className = "";
    private String subtypeName = "";
    private String packageName = "";
    private boolean includeSubpackages = false;
    private String excludedPackageName = "";
    private boolean excludeSubpackages = false;

    private List<MethodScanBuilder> methods = new ArrayList<>();
    private List<FieldScanBuilder> fields = new ArrayList<>();
    private List<AnnotationScanBuilder> annotations = new ArrayList<>();

    private ClassScanBuilder () { }

    public static ClassScanBuilder create() {
        return new ClassScanBuilder();
    }

    public ClassScanBuilder className(String className) {
        this.className = className;
        return this;
    }

    public ClassScanBuilder subtype(Class<?> clazz) {
        this.subtypeName = clazz.getTypeName();
        return this;
    }

    public ClassScanBuilder subtypeName(String subtypeName) {
        this.subtypeName = subtypeName;
        return this;
    }

    public ClassScanBuilder packageName(String name) {
        this.packageName = packageName;
        return this;
    }

    public ClassScanBuilder includeSubpackages(boolean includeSubpackages) {
        this.includeSubpackages = includeSubpackages;
        return this;
    }

    public ClassScanBuilder excludedPackageName(String excludedPackageName) {
        this.excludedPackageName = excludedPackageName;
        return this;
    }

    public ClassScanBuilder excludeSubpackages(boolean excludeSubpackages) {
        this.excludeSubpackages = excludeSubpackages;
        return this;
    }

    public ClassScanBuilder hasMethod(MethodScanBuilder methodScanBuilder) {
        methods.add(methodScanBuilder);
        return this;
    }

    public ClassScanBuilder hasField(FieldScanBuilder fieldScanBuilder) {
        fields.add(fieldScanBuilder);
        return this;
    }

    public ClassScanBuilder hasAnnotation(AnnotationScanBuilder annotationScanBuilder) {
        annotations.add(annotationScanBuilder);
        return this;
    }
}
