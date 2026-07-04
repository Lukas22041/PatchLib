package patchlib.agent.dispatch;

import patchlib.agent.PatchHandler;
import patchlib.agent.PatchLibLogger;
import patchlib.agent.PatchRegistry;
import patchlib.agent.PatchSite;
import patchlib.agent.RedirectSite;
import patchlib.agent.context.AdviceContextImpl;
import patchlib.agent.context.RedirectContextImpl;
import patchlib.agent.spec.RedirectKind;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;

/** Builds the composed handler chains behind the dynamic constants of constant dispatch sites.
 * The JVM calls a bootstrap once per constant on the first execution of the patched method and caches
 * the result in the constant pool, making it a full constant to the JIT. Bound arguments of a
 * constant MethodHandle are constants themselves, which is what lets the handler bodies inline. */
public final class ChainBootstrap {

    private ChainBootstrap() {}

    private static final MethodType CHAIN_TYPE = MethodType.methodType(void.class, AdviceContextImpl.class);

    /** Every redirect chain link shares this shape: the intercepted access target (call receiver or
     * field owner, null when absent), the access arguments, and the host method's self and args. */
    private static final MethodType REDIRECT_CHAIN_TYPE = MethodType.methodType(Object.class,
            Object.class, Object[].class, Object.class, Object[].class);

    private static final MethodHandle INVOKE_LOGGED;
    private static final MethodHandle RUN_LAYER;

    /** Reads element 0 of an Object[], used to unpack the written value for field write accesses. */
    private static final MethodHandle FIRST_ELEMENT =
            MethodHandles.insertArguments(MethodHandles.arrayElementGetter(Object[].class), 1, 0);

    static {
        try {
            INVOKE_LOGGED = MethodHandles.lookup().findStatic(ChainBootstrap.class, "invokeLogged",
                    MethodType.methodType(void.class, MethodHandle.class, String.class, AdviceContextImpl.class));
            RUN_LAYER = MethodHandles.lookup().findStatic(ChainBootstrap.class, "runLayer",
                    MethodType.methodType(Object.class, MethodHandle.class, String.class, MethodHandle.class, Class.class,
                            Object.class, Object[].class, Object.class, Object[].class));
        } catch (NoSuchMethodException | IllegalAccessException e) {
            throw new RuntimeException("Could not resolve the chain invokers", e);
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
        return MethodHandles.insertArguments(INVOKE_LOGGED, 0, handler.handler(), blame(handler));
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

    private static String blame(PatchHandler handler) {
        return "Ran in to an error while dispatcher was executing "
                + handler.spec().handlerClass() + "#" + handler.spec().handlerMethod()
                + " from mod " + handler.spec().sourceMod().getId();
    }

    /** Constant dispatch bootstrap for redirect chains, one constant per intercepted call site. The original access
     * arrives as a method handle constant that the host class resolved itself, so it carries the host's
     * access rights. Layer wrapping failures drop the layers and keep the original access behavior. */
    public static MethodHandle redirectBootstrap(MethodHandles.Lookup lookup, String name, Class<?> type,
                                                 int siteId, Class<?> hostOwner, MethodHandle original, int hasReceiver) {
        RedirectSite site = PatchRegistry.redirectSite(siteId);
        MethodHandle access = adaptOriginal(site.kind(), original, hasReceiver == 1);
        try {
            MethodHandle chain = access;
            PatchHandler[] layers = site.layers();
            //Wrap from the innermost layer out, so layer 0 (lowest priority) runs first and each
            //layer's ctx.call() reaches the link below it.
            for (int i = layers.length - 1; i >= 0; i--) {
                chain = MethodHandles.insertArguments(RUN_LAYER, 0, layers[i].handler(), blame(layers[i]), chain, hostOwner);
            }
            return chain;
        } catch (Throwable t) {
            PatchLibLogger.error("Could not build the redirect chain for site " + siteId
                    + ", its layers are dropped: " + t);
            return access;
        }
    }

    /** One redirect layer, bound in to the chain with its handler, blame, next link and host class.
     * Only blames this layer when the layer itself threw. Exceptions surfacing from ctx.call() (a deeper
     * layer or the original access) come back through proceed, which records them on the context, so they
     * keep propagating without being double-logged or pinned on the wrong mod. */
    static Object runLayer(MethodHandle handler, String blame, MethodHandle next, Class<?> hostOwner,
                           Object target, Object[] args, Object hostSelf, Object[] hostArgs) {
        RedirectContextImpl ctx = new RedirectContextImpl(hostOwner, hostSelf, hostArgs, target, args, next);
        try {
            handler.invokeExact(ctx);
        } catch (Throwable ex) {
            if (!ctx.isPropagating(ex)) PatchLibLogger.error(blame);
            throw PatchDispatcher.uncheckedThrow(ex);
        }
        return ctx.getResult();
    }

    /** Adapts the original access handle to the chain shape. The generic asType form plus invokeExact
     * skips the per call type checking and boxing setup that invokeWithArguments redoes on every access.
     * The receiver stays a leading positional parameter, so no argument array is rebuilt per call. */
    private static MethodHandle adaptOriginal(RedirectKind kind, MethodHandle original, boolean hasReceiver) {
        int count = original.type().parameterCount();
        MethodHandle generic = original.asType(MethodType.genericMethodType(count));
        MethodHandle access = switch (kind) {
            case METHOD_CALL, CONSTRUCTOR -> hasReceiver //a constructor never has one: the handle allocates and initializes in one step
                    ? generic.asSpreader(Object[].class, count - 1)
                    : MethodHandles.dropArguments(generic.asSpreader(Object[].class, count), 0, Object.class);
            case FIELD_READ -> hasReceiver
                    ? MethodHandles.dropArguments(generic, 1, Object[].class)
                    : MethodHandles.dropArguments(generic, 0, Object.class, Object[].class);
            //A write has no result, the adapted handle returns the null that asType makes of the void return.
            case FIELD_WRITE -> hasReceiver
                    ? MethodHandles.filterArguments(generic, 1, FIRST_ELEMENT)
                    : MethodHandles.dropArguments(MethodHandles.filterArguments(generic, 0, FIRST_ELEMENT), 0, Object.class);
        };
        //The original never reads the host values, they ride along for the layers above it.
        return MethodHandles.dropArguments(access, 2, Object.class, Object[].class);
    }
}
