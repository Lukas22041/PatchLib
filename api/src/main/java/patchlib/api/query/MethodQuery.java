package patchlib.api.query;

import patchlib.api.match.MethodType;
import patchlib.api.match.StaticType;
import patchlib.api.spec.AnnotationQuerySpec;
import patchlib.api.spec.FieldQuerySpec;
import patchlib.api.spec.MethodQuerySpec;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MethodQuery {

    private String methodName = "";
    private List<String> parameterTypeNames = new ArrayList<>();
    private List<String> containsParameterType = new ArrayList<>();
    private int parameterCount = -1;
    private String returnTypeName = "";
    private MethodType methodType = MethodType.ANY;
    private StaticType staticType = StaticType.ANY;
    private List<AnnotationQuery> annotations = new ArrayList<>();

    private MethodQuery() { }

    public static MethodQuery create() {
        return new MethodQuery();
    }

    public MethodQuerySpec build() {
        List<AnnotationQuerySpec> annotationSpecs = annotations.stream().map(AnnotationQuery::build).toList();
        return new MethodQuerySpec(methodName, parameterTypeNames, containsParameterType, parameterCount, returnTypeName, methodType, staticType, annotationSpecs);
    }

    public MethodQuery methodName(String methodName) {
        this.methodName = methodName;
        return this;
    }

    public MethodQuery parameterTypes(Class<?>... parameterTypes) {
        this.parameterTypeNames = Arrays.stream(parameterTypes).map(Class::getTypeName).toList();
        return this;
    }

    public MethodQuery parameterTypeNames(String... parameterTypeNames) {
        this.parameterTypeNames = Arrays.stream(parameterTypeNames).toList();
        return this;
    }

    public MethodQuery containsParameterType(Class<?> parameterType) {
        this.containsParameterType.add(parameterType.getTypeName());
        return this;
    }

    public MethodQuery containsParameterTypeName(String parameterTypeName) {
        this.containsParameterType.add(parameterTypeName);
        return this;
    }

    public MethodQuery parameterCount(int parameterCount) {
        this.parameterCount = parameterCount;
        return this;
    }

    public MethodQuery returnType(Class<?> returnType) {
        this.returnTypeName = returnType.getTypeName();
        return this;
    }

    public MethodQuery returnTypeName(String returnTypeName) {
        this.returnTypeName = returnTypeName;
        return this;
    }

    public MethodQuery methodType(MethodType methodType) {
        this.methodType = methodType;
        return this;
    }

    public MethodQuery staticType(StaticType staticType) {
        this.staticType = staticType;
        return this;
    }

    public MethodQuery hasAnnotation(AnnotationQuery annotationQuery) {
        this.annotations.add(annotationQuery);
        return this;
    }
}
