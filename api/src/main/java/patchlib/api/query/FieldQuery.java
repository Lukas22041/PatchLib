package patchlib.api.query;

import patchlib.api.spec.AnnotationQuerySpec;
import patchlib.api.spec.FieldQuerySpec;

import java.util.ArrayList;
import java.util.List;

public class FieldQuery {

    private String fieldName = "";
    private String fieldTypeName = "";
    private String fieldSubtypeName = "";
    private boolean staticOnly = false;
    private List<AnnotationQuery> annotations = new ArrayList<>();

    private FieldQuery() { }

    public static FieldQuery create() {
        return new FieldQuery();
    }

    public FieldQuerySpec build() {
        List<AnnotationQuerySpec> annotationSpecs = annotations.stream().map(AnnotationQuery::build).toList();
        return new FieldQuerySpec(fieldName, fieldTypeName, fieldSubtypeName, staticOnly, annotationSpecs);
    }

    public FieldQuery fieldName(String fieldName) {
        this.fieldName = fieldName;
        return this;
    }

    public FieldQuery fieldType(Class<?> fieldType) {
        this.fieldTypeName = fieldType.getTypeName();
        return this;
    }

    public FieldQuery fieldTypeName(String fieldTypeName) {
        this.fieldTypeName = fieldTypeName;
        return this;
    }

    public FieldQuery fieldSubtype(Class<?> fieldSubtype) {
        this.fieldSubtypeName = fieldSubtype.getTypeName();
        return this;
    }

    public FieldQuery fieldSubtypeName(String fieldSubtypeName) {
        this.fieldSubtypeName = fieldSubtypeName;
        return this;
    }

    public FieldQuery staticOnly(boolean staticOnly) {
        this.staticOnly = staticOnly;
        return this;
    }

    public FieldQuery hasAnnotation(AnnotationQuery annotationQuery) {
        this.annotations.add(annotationQuery);
        return this;
    }
}
