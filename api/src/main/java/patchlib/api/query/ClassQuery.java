package patchlib.api.query;

import java.util.ArrayList;
import java.util.List;

public class ClassQuery {

    private String className = "";
    private String subtypeName = "";
    private String packageName = "";
    private boolean includeSubpackages = false;
    private String excludedPackageName = "";
    private boolean excludeSubpackages = false;

    private List<MethodQuery> methods = new ArrayList<>();
    private List<FieldQuery> fields = new ArrayList<>();
    private List<AnnotationQuery> annotations = new ArrayList<>();

    private ClassQuery() { }

    public static ClassQuery create() {
        return new ClassQuery();
    }

    public ClassQuery className(String className) {
        this.className = className;
        return this;
    }

    public ClassQuery subtype(Class<?> clazz) {
        this.subtypeName = clazz.getTypeName();
        return this;
    }

    public ClassQuery subtypeName(String subtypeName) {
        this.subtypeName = subtypeName;
        return this;
    }

    public ClassQuery packageName(String name) {
        this.packageName = packageName;
        return this;
    }

    public ClassQuery includeSubpackages(boolean includeSubpackages) {
        this.includeSubpackages = includeSubpackages;
        return this;
    }

    public ClassQuery excludedPackageName(String excludedPackageName) {
        this.excludedPackageName = excludedPackageName;
        return this;
    }

    public ClassQuery excludeSubpackages(boolean excludeSubpackages) {
        this.excludeSubpackages = excludeSubpackages;
        return this;
    }

    public ClassQuery hasMethod(MethodQuery methodScanBuilder) {
        methods.add(methodScanBuilder);
        return this;
    }

    public ClassQuery hasField(FieldQuery fieldQuery) {
        fields.add(fieldQuery);
        return this;
    }

    public ClassQuery hasAnnotation(AnnotationQuery annotationQuery) {
        annotations.add(annotationQuery);
        return this;
    }
}
