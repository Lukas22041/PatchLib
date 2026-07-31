package patchlib.api.spec;

import patchlib.api.query.AnnotationQuery;
import patchlib.api.query.ClassQuery;
import patchlib.api.query.FieldQuery;
import patchlib.api.query.MethodQuery;

import java.util.List;

public record ClassQuerySpec(
        String className,
        String subtypeName,
        String packageName,
        boolean includeSubpackages,
        String excludedPackageName,
        boolean excludeSubpackages,
        List<MethodQuerySpec>methods,
        List<FieldQuerySpec> fields,
        List<AnnotationQuerySpec> annotations
) {


    public ClassQuerySpec {
        methods = List.copyOf(methods);
        fields = List.copyOf(fields);
        annotations = List.copyOf(annotations);
    }
}
