package patchlib.api.spec;

import patchlib.api.match.MethodType;
import patchlib.api.query.AnnotationQuery;

import java.util.ArrayList;
import java.util.List;

public record MethodQuerySpec(
    String methodName,
    List<String> parameterTypeNames,
    List<String> containsParameterType,
    int parameterCount,
    String returnTypeName,
    MethodType methodType,
    boolean staticOnly,
    List<AnnotationQuerySpec> annotations
) {

}
