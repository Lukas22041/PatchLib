package patchlib.agent.spec;

import com.fs.starfarer.api.ModSpecAPI;
import patchlib.agent.context.*;
import patchlib.api.context.*;
import patchlib.api.spec.ClassQuerySpec;
import patchlib.api.spec.MethodQuerySpec;

/** Represents one patch handler */
public record PatchHandlerSpec(
        String handlerClassName,
        String handlerMethodName,
        ModSpecAPI sourceMod,
        int priority,
        ClassQuerySpec targetClass,
        MethodQuerySpec targetMethod,
        PatchSpec patchSpec
) {

    public enum RedirectType {
        METHOD_CALL, CONSTRUCTOR, FIELD_WRITE, FIELD_READ
    }

    public boolean isAdvice() {
        return patchSpec instanceof AdviceSpec;
    }

    public boolean isRedirect() {
        return !isAdvice();
    }

    public RedirectType getRedirectType() {
        if (patchSpec instanceof RedirectMethodCallSpec) return RedirectType.METHOD_CALL;
        else if (patchSpec instanceof RedirectConstructorCallSpec) return RedirectType.CONSTRUCTOR;
        else if (patchSpec instanceof RedirectFieldReadSpec) return RedirectType.FIELD_READ;
        else if (patchSpec instanceof RedirectFieldWriteSpec) return RedirectType.FIELD_WRITE;
        throw new IllegalStateException("getRedirectType called on non-redirect spec");
    }

    public Class<?> getContextClass() {
        if (patchSpec instanceof AdviceSpec adviceSpec) {
            return switch (adviceSpec.adviceType()) {
                case BEFORE -> BeforeContext.class;
                case AFTER -> AfterContext.class;
                case EXCEPT -> ExceptContext.class;
            };
        } else {
            if (patchSpec instanceof RedirectMethodCallSpec) return MethodCallContext.class;
            else if (patchSpec instanceof RedirectConstructorCallSpec) return ConstructorCallContext.class;
            else if (patchSpec instanceof RedirectFieldReadSpec) return FieldReadContext.class;
            else if (patchSpec instanceof RedirectFieldWriteSpec) return FieldWriteContext.class;
        }
        throw new IllegalStateException("Unknown patch spec type " + patchSpec);
    }

    public Class<?> getContextImplClass() {
        if (isAdvice()) return HookContextImpl.class;
        else if (patchSpec instanceof RedirectMethodCallSpec) return MethodCallContextImpl.class;
        else if (patchSpec instanceof RedirectConstructorCallSpec) return ConstructorCallContextImpl.class;
        else if (patchSpec instanceof RedirectFieldReadSpec) return FieldReadContextImpl.class;
        else if (patchSpec instanceof RedirectFieldWriteSpec) return FieldWriteContextImpl.class;
        throw new IllegalStateException("Unknown patch spec type " + patchSpec);
    }

}
