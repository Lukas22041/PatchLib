package patch_lib.agent.data;

public record PatchSpec(String handlerClass, String handlerMethod, TargetClassSpec targetClass, TargetMethodSpec targetMethod) {

}
