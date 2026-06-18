package patch_lib.agent.matchers;

import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.method.ParameterList;
import net.bytebuddy.matcher.ElementMatcher;
import patch_lib.agent.spec.TargetMethodSpec;

import static net.bytebuddy.matcher.ElementMatchers.*;

public class MethodTargetMatcher {

    public static ElementMatcher.Junction<MethodDescription> create(TargetMethodSpec methodSpec) {

        ElementMatcher.Junction<MethodDescription> matcher = switch (methodSpec.methodType()) {
            case METHOD      -> isMethod();
            case CONSTRUCTOR -> isConstructor();
            case ANY         -> isMethod().or(isConstructor());
        };

        if (!methodSpec.methodName().isEmpty())
            matcher = matcher.and(named(methodSpec.methodName()));

        if (methodSpec.parameters().length > 0) {
            String[] params = methodSpec.parameters();
            matcher = matcher.and((MethodDescription methodDesc) -> {
                ParameterList<?> parameterList = methodDesc.getParameters();
                if (parameterList.size() != params.length) return false;
                for (int i = 0; i < params.length; i++)
                    if (!parameterList.get(i).getType().asErasure().getActualName().equals(params[i])) return false;
                return true;
            });
        } else if (methodSpec.parameterCount() >= 0) {
            int count = methodSpec.parameterCount();
            matcher = matcher.and((MethodDescription methodDesc) -> methodDesc.getParameters().size() == count);
        }

        if (!methodSpec.returnType().isEmpty())
            matcher = matcher.and(returns(named(methodSpec.returnType())));

        if (methodSpec.staticOnly())
            matcher = matcher.and(isStatic());

        return matcher;
    }

}
