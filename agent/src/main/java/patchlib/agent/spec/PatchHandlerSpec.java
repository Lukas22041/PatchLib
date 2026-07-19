package patchlib.agent.spec;

import com.fs.starfarer.api.ModSpecAPI;
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

}
