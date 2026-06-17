package patch_lib.agent.spec;

import com.fs.starfarer.api.ModSpecAPI;

public record PatchSpec(
        ModSpecAPI sourceMod,
        String handlerClass,
        String handlerMethod,
        PatchType patchType,
        int priority,
        TargetClassSpec targetClass,
        TargetMethodSpec targetMethod) {

}
