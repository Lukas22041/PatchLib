package patchlib.api.spec;

import patchlib.api.match.StaticType;
import patchlib.api.query.AnnotationQuery;

import java.util.ArrayList;
import java.util.List;

public record FieldQuerySpec(
        String fieldName,
        String typeName,
        String subtypeName,
        StaticType staticType,
        List<AnnotationQuerySpec>annotations
) {

    public FieldQuerySpec {
        annotations = List.copyOf(annotations);
    }

}
