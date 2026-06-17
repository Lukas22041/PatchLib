package patch_lib.agent.spec;

public record PatchSpec(
        String modId,
        String handlerClass,
        String handlerMethod,
        PatchType patchType,
        int priority,
        TargetClassSpec targetClass,
        TargetMethodSpec targetMethod) {

}
