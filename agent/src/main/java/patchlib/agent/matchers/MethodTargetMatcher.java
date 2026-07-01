package patchlib.agent.matchers;

import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.method.ParameterList;
import net.bytebuddy.matcher.ElementMatcher;
import patchlib.agent.spec.RedirectSiteSpec;
import patchlib.agent.spec.TargetMethodSpec;

import static net.bytebuddy.matcher.ElementMatchers.*;

public class MethodTargetMatcher {

    public static ElementMatcher.Junction<MethodDescription> create(TargetMethodSpec spec) {
        ElementMatcher.Junction<MethodDescription> base = switch (spec.methodType()) {
            case METHOD      -> isMethod();
            case CONSTRUCTOR -> isConstructor();
            case ANY         -> isMethod().or(isConstructor());
        };
        return refine(base, spec.methodName(), spec.parameters(), spec.parameterCount(), spec.returnType(), spec.staticOnly());
    }

    /** Matches the method call a @RedirectCall intercepts inside the host body. Same shape matching as a target method
     * (a call is never a constructor, so isMethod), plus the owner, which is the class declaring the called method. */
    public static ElementMatcher.Junction<MethodDescription> create(RedirectSiteSpec spec) {
        ElementMatcher.Junction<MethodDescription> base = isMethod();
        if (!spec.owner().matchesEverything())
            base = base.and(isDeclaredBy(ClassTargetMatcher.create(spec.owner())));
        return refine(base, spec.name(), spec.parameters(), spec.parameterCount(), spec.returnOrFieldType(), spec.staticOnly());
    }

    /** Adds the name/parameter/return/static constraints shared by target methods and redirected calls onto a base matcher. */
    private static ElementMatcher.Junction<MethodDescription> refine(ElementMatcher.Junction<MethodDescription> matcher,
                                                                     String name, String[] parameters, int parameterCount,
                                                                     String returnType, boolean staticOnly) {
        if (!name.isEmpty())
            matcher = matcher.and(named(name));

        if (parameters.length > 0) {
            matcher = matcher.and((MethodDescription methodDesc) -> {
                ParameterList<?> parameterList = methodDesc.getParameters();
                if (parameterList.size() != parameters.length) return false;
                for (int i = 0; i < parameters.length; i++)
                    if (!parameterList.get(i).getType().asErasure().getActualName().equals(parameters[i])) return false;
                return true;
            });
        } else if (parameterCount >= 0) {
            matcher = matcher.and((MethodDescription methodDesc) -> methodDesc.getParameters().size() == parameterCount);
        }

        if (!returnType.isEmpty())
            matcher = matcher.and(returns(named(returnType)));

        if (staticOnly)
            matcher = matcher.and(isStatic());

        return matcher;
    }

}
