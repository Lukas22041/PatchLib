package patchlib.agent.dispatch;

import patchlib.agent.PatchHandler;
import patchlib.agent.PatchLibLogger;
import patchlib.agent.PatchRegistry;
import patchlib.agent.PatchSite;
import patchlib.agent.context.AdviceContextImpl;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;

/** Builds the composed handler chains behind the dynamic constants of constant dispatch sites.
 * The JVM calls bootstrap once per constant on the first execution of the patched method and caches
 * the result in the constant pool, making it a full constant to the JIT. Bound arguments of a
 * constant MethodHandle are constants themselves, which is what lets the handler bodies inline. */
public final class ChainBootstrap {

    private ChainBootstrap() {}

    private static final MethodType CHAIN_TYPE = MethodType.methodType(void.class, AdviceContextImpl.class);

    private static final MethodHandle INVOKE_LOGGED;

    static {
        try {
            INVOKE_LOGGED = MethodHandles.lookup().findStatic(ChainBootstrap.class, "invokeLogged",
                    MethodType.methodType(void.class, MethodHandle.class, String.class, AdviceContextImpl.class));
        } catch (NoSuchMethodException | IllegalAccessException e) {
            throw new RuntimeException("Could not resolve the chain invoker", e);
        }
    }

    /** Constant dynamic bootstrap for advice chains. The constant's name selects the phase.
     * Must never throw, a throwing bootstrap would break the host method on its first call,
     * so a failed build drops the phase's patches and keeps the host behavior. */
    public static MethodHandle bootstrap(MethodHandles.Lookup lookup, String name, Class<?> type, int siteId) {
        try {
            PatchSite site = PatchRegistry.site(siteId);
            PatchHandler[] handlers = switch (name) {
                case "before" -> site.beforePatches();
                case "after" -> site.afterPatches();
                default -> throw new IllegalArgumentException("Unknown chain name " + name);
            };
            return compose(handlers);
        } catch (Throwable t) {
            PatchLibLogger.error("Could not build the " + name + " chain for site " + siteId
                    + ", its patches are dropped: " + t);
            return MethodHandles.empty(CHAIN_TYPE);
        }
    }

    /** One handle that runs the handlers in array order. foldArguments(target, combiner) runs the
     * combiner first with the same argument, so folding from the back keeps the order. */
    private static MethodHandle compose(PatchHandler[] handlers) {
        if (handlers.length == 0) return MethodHandles.empty(CHAIN_TYPE);

        MethodHandle chain = wrap(handlers[handlers.length - 1]);
        for (int i = handlers.length - 2; i >= 0; i--) {
            chain = MethodHandles.foldArguments(chain, wrap(handlers[i]));
        }
        return chain;
    }

    /** Binds one handler and its blame message in to the invoker, leaving (AdviceContextImpl)void. */
    private static MethodHandle wrap(PatchHandler handler) {
        String blame = "Ran in to an error while dispatcher was executing "
                + handler.spec().handlerClass() + "#" + handler.spec().handlerMethod()
                + " from mod " + handler.spec().sourceMod().getId();
        return MethodHandles.insertArguments(INVOKE_LOGGED, 0, handler.handler(), blame);
    }

    /** Constant dispatch twin of PatchDispatcher.invoke. */
    static void invokeLogged(MethodHandle handler, String blame, AdviceContextImpl context) {
        try {
            handler.invokeExact(context);
        } catch (Throwable ex) {
            PatchLibLogger.error(blame);
            throw PatchDispatcher.uncheckedThrow(ex);
        }
    }
}
