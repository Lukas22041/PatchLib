package patchlib.agent.patch.advice;

import patchlib.agent.context.HookContextImpl;

import java.lang.invoke.MethodHandle;

public class AdviceDispatcher {

    public static HookContextImpl enter(int siteId, MethodHandle beforeHandle, Class<?> owner, Object self, Object[] args) {
        //Will be null for Janino loaded classes
        if (beforeHandle == null) {
            beforeHandle = AdvicePatchRegistry.getSite(siteId).beforeChain;
        }

        HookContextImpl hookContext = new HookContextImpl(owner, self, args, siteId);
        invoke(beforeHandle, hookContext);
        return hookContext;
    }

    public static Object exit(int siteId, MethodHandle afterHandle, HookContextImpl context, Object returned) {
        //Will be null for Janino loaded classes
        if (afterHandle == null) {
            afterHandle = AdvicePatchRegistry.getSite(siteId).afterChain;
        }

        if (!context.isSkipOriginal()) {
            context.setReturnValue(returned);
        }

        invoke(afterHandle, context);
        return context.getReturnValue();
    }

    public static Throwable except(int siteId, HookContextImpl context, Throwable thrown) {
        MethodHandle exceptHandle = AdvicePatchRegistry.getSite(siteId).exceptChain;
        context.initThrown(thrown);
        invoke(exceptHandle, context);
        return context.getThrown();
    }

    @SuppressWarnings("unchecked")
    private static <T extends Throwable> void invoke(MethodHandle handle, HookContextImpl hookContext) throws T{
        try {
            handle.invokeExact(hookContext);
        } catch (Throwable ex) {
            throw (T) ex;
        }
    }
}
