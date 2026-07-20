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

    public boolean isAdvice() {
        return patchSpec instanceof AdviceSpec;
    }

    public boolean isRedirect() {
        return !isAdvice();
    }

    public Class<?> getContextClass() {
        if (patchSpec instanceof AdviceSpec adviceSpec) {
            return switch (adviceSpec.adviceType()) {
                case BEFORE -> BeforeContext.class;
                case AFTER -> AfterContext.class;
                case EXCEPT -> ExceptContext.class;
            };
        } else {
            if (patchSpec instanceof RedirectCallSpec) return MethodCallContext.class;
            else if (patchSpec instanceof RedirectNewSpec) return ConstructorCallContext.class;
            else if (patchSpec instanceof RedirectFieldReadSpec) return FieldReadContext.class;
            else if (patchSpec instanceof RedirectFieldWriteSpec) return FieldWriteContext.class;
        }
        return null;
    }

    public Class<?> getContextImplClass() {
        if (isAdvice()) return HookContextImpl.class;
        else if (patchSpec instanceof RedirectCallSpec) return MethodCallContextImpl.class;
        else if (patchSpec instanceof RedirectNewSpec) return ConstructorCallContextImpl.class;
        else if (patchSpec instanceof RedirectFieldReadSpec) return FieldReadContextImpl.class;
        else if (patchSpec instanceof RedirectFieldWriteSpec) return FieldWriteContextImpl.class;
        return null;
    }

}
