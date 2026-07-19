package patchlib.agent.matchers;

import net.bytebuddy.description.annotation.AnnotationDescription;
import net.bytebuddy.description.annotation.AnnotationSource;
import net.bytebuddy.description.field.FieldDescription;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.method.ParameterList;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.matcher.ElementMatcher;
import patchlib.api.match.MethodType;
import patchlib.api.spec.AnnotationQuerySpec;
import patchlib.api.spec.ClassQuerySpec;
import patchlib.api.spec.FieldQuerySpec;
import patchlib.api.spec.MethodQuerySpec;

import java.util.List;
import java.util.stream.Collectors;

import static net.bytebuddy.matcher.ElementMatchers.*;

public class MethodMatcher {

    public static ElementMatcher.Junction<MethodDescription> fromQuery(MethodQuerySpec query) {
        return fromQuery(query, null);
    }

    public static ElementMatcher.Junction<MethodDescription> fromQuery(MethodQuerySpec query, ClassQuerySpec ownerQuery) {
        ElementMatcher.Junction<MethodDescription> matcher = any();

        if (!query.methodName().isEmpty()) {
            matcher = matcher.and(named(query.methodName()));
        }

        if (!query.parameterTypeNames().isEmpty()) {
            matcher = matcher.and(description -> matchesParameters(query, description));
        } else if (query.parameterCount() >= 0) {
            matcher = matcher.and(description -> description.getParameters().size() == query.parameterCount());
        }

        if (!query.containsParameterType().isEmpty()) {
            matcher = matcher.and(description -> containsParameters(query, description));
        }

        if (!query.returnTypeName().isEmpty()) {
            matcher = matcher.and(returns(named(query.returnTypeName())));
        }

        switch (query.methodType()) {
            case METHOD -> matcher = matcher.and(isMethod());
            case CONSTRUCTOR -> matcher = matcher.and(isConstructor());
            case ANY -> matcher = matcher.and(isMethod().or(isConstructor()));
        }

        if (query.staticOnly()) {
            matcher = matcher.and(isStatic());
        }

        for (AnnotationQuerySpec annotation : query.annotations()) {
            ElementMatcher.Junction<AnnotationDescription> annotationMatcher = AnnotationMatcher.fromQuery(annotation);
            matcher = matcher.and(declaresAnnotation(annotationMatcher));
        }

        //Check if the method is part of some class, used only by redirects.
        if (ownerQuery != null) {
            ElementMatcher.Junction<TypeDescription> ownerMatcher = ClassMatcher.fromQuery(ownerQuery);
            matcher = matcher.and(isDeclaredBy(ownerMatcher));
        }

        return matcher;
    }

    private static boolean matchesParameters(MethodQuerySpec query, MethodDescription description) {
        ParameterList<?> parameters = description.getParameters();
        List<String> parameterNames = query.parameterTypeNames();

        if (parameters.size() != parameterNames.size()) return false;

        for (int i = 0; i < parameters.size(); i++) {
            String parameter = parameters.get(i).getType().asErasure().getActualName();
            String parameterName = parameterNames.get(i);
            if (!parameter.equals(parameterName)) {
                return false;
            }
        }

        return true;
    }

    private static boolean containsParameters(MethodQuerySpec query, MethodDescription description) {
        List<String> parameters = description.getParameters().stream().map(param -> param.getType().asErasure().getActualName()).collect(Collectors.toList());
        List<String> parametersToCheck = query.containsParameterType();

        for (String parameterToCheck : parametersToCheck) {
            if (!parameters.contains(parameterToCheck)) {
                return false;
            }
            parameters.remove(parameterToCheck);
        }

        return true;
    }

}
