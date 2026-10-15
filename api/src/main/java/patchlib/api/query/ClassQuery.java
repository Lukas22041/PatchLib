package patchlib.api.query;

import patchlib.api.spec.AnnotationQuerySpec;
import patchlib.api.spec.ClassQuerySpec;
import patchlib.api.spec.FieldQuerySpec;
import patchlib.api.spec.MethodQuerySpec;

import java.util.ArrayList;
import java.util.List;

public class ClassQuery {

    private String typeName = "";
    private String subtypeName = "";
    private String packageName = "";
    private boolean packageRecursive = false;
    private String excludedPackageName = "";
    private boolean excludedPackageRecursive = false;

    private List<MethodQuery> methods = new ArrayList<>();
    private List<FieldQuery> fields = new ArrayList<>();
    private List<AnnotationQuery> annotations = new ArrayList<>();

    private ClassQuery() { }

    public static ClassQuery create() {
        return new ClassQuery();
    }

    public ClassQuerySpec build() {
        List<MethodQuerySpec> methodSpecs = methods.stream().map(MethodQuery::build).toList();
        List<FieldQuerySpec> fieldSpecs = fields.stream().map(FieldQuery::build).toList();
        List<AnnotationQuerySpec> annotationSpecs = annotations.stream().map(AnnotationQuery::build).toList();

        return new ClassQuerySpec(typeName, subtypeName, packageName, packageRecursive, excludedPackageName, excludedPackageRecursive,
                methodSpecs, fieldSpecs, annotationSpecs);
    }

    public ClassQuery type(Class<?> clazz) {
        this.typeName = clazz.getTypeName();
        return this;
    }

    public ClassQuery typeName(String typeName) {
        this.typeName = typeName;
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

    public ClassQuery packageName(String packageName) {
        this.packageName = packageName;
        return this;
    }

    public ClassQuery packageRecursive(boolean packageRecursive) {
        this.packageRecursive = packageRecursive;
        return this;
    }

    public ClassQuery excludedPackageName(String excludedPackageName) {
        this.excludedPackageName = excludedPackageName;
        return this;
    }

    public ClassQuery excludedPackageRecursive(boolean excludedPackageRecursive) {
        this.excludedPackageRecursive = excludedPackageRecursive;
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
