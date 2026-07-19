package patchlib.api.spec;

import patchlib.api.query.AnnotationQuery;

import java.util.ArrayList;
import java.util.List;

public record FieldQuerySpec(
        String fieldName,
        String fieldTypeName,
        String fieldSubtypeName,
        boolean staticOnly,
        List<AnnotationQuerySpec>annotations
) {

    public FieldQuerySpec {
        annotations = List.copyOf(annotations);
    }

}
