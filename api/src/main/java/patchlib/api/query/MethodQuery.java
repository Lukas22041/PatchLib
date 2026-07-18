package patchlib.api.query;

import patchlib.api.match.MethodType;

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
    boolean staticOnly = false;
    private List<AnnotationQuery> annotations = new ArrayList<>();

    private MethodQuery() { }

    public static MethodQuery create() {
        return new MethodQuery();
    }

    public MethodQuery methodName(String methodName) {
        this.methodName = methodName;
        return this;
    }

    public MethodQuery parameters(Class<?>... parameterTypes) {
        this.parameterTypeNames = Arrays.stream(parameterTypes).map(Class::getTypeName).toList();
        return this;
    }

    public MethodQuery parameterNames(String... parameterTypeNames) {
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

    public MethodQuery staticOnly(boolean staticOnly) {
        this.staticOnly = staticOnly;
        return this;
    }

    public MethodQuery hasAnnotation(AnnotationQuery annotationQuery) {
        this.annotations.add(annotationQuery);
        return this;
    }
}
