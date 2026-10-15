package patchlib.api.query;

import patchlib.api.match.StaticType;
import patchlib.api.spec.AnnotationQuerySpec;
import patchlib.api.spec.FieldQuerySpec;

import java.util.ArrayList;
import java.util.List;

public class FieldQuery {

    private String fieldName = "";
    private String typeName = "";
    private String subtypeName = "";
    private StaticType staticType = StaticType.ANY;
    private List<AnnotationQuery> annotations = new ArrayList<>();

    private FieldQuery() { }

    public static FieldQuery create() {
        return new FieldQuery();
    }

    public FieldQuerySpec build() {
        List<AnnotationQuerySpec> annotationSpecs = annotations.stream().map(AnnotationQuery::build).toList();
        return new FieldQuerySpec(fieldName, typeName, subtypeName, staticType, annotationSpecs);
    }

    public FieldQuery fieldName(String fieldName) {
        this.fieldName = fieldName;
        return this;
    }

    public FieldQuery type(Class<?> type) {
        this.typeName = type.getTypeName();
        return this;
    }

    public FieldQuery typeName(String typeName) {
        this.typeName = typeName;
        return this;
    }

    public FieldQuery subtype(Class<?> subtype) {
        this.subtypeName = subtype.getTypeName();
        return this;
    }

    public FieldQuery subtypeName(String subtypeName) {
        this.subtypeName = subtypeName;
        return this;
    }

    public FieldQuery staticType(StaticType staticType) {
        this.staticType = staticType;
        return this;
    }

    public FieldQuery hasAnnotation(AnnotationQuery annotationQuery) {
        this.annotations.add(annotationQuery);
        return this;
    }
}
