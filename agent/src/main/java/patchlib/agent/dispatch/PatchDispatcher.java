package patchlib.agent.dispatch;

import patchlib.agent.PatchHandler;
import patchlib.agent.PatchLibLogger;
import patchlib.agent.PatchRegistry;
import patchlib.agent.PatchSite;
import patchlib.agent.RedirectSite;
import patchlib.agent.context.AdviceContextImpl;
import patchlib.agent.context.RedirectContextImpl;

import java.lang.invoke.MethodHandle;

/** Class that handles dispatching patches. PatchInstaller inserts bytecode in to every patched class,
 * which uses this dispatcher to delegate work to the patch handlers. */
public class PatchDispatcher {

    public static AdviceContextImpl enter(int siteId, Class<?> owner, Object self, Object[] args) {
        PatchSite site = PatchRegistry.site(siteId);
        AdviceContextImpl context = new AdviceContextImpl(owner, self, args);
        for (PatchHandler patch : site.beforePatches()) {
            invoke(patch, context);
        }
        return context;
    }

    /** Constant dispatch twin of enter. The chain arrives as a dynamic constant at the advice
     * call site, so the JIT can inline it and the handlers it contains, see ChainBootstrap. */
    public static AdviceContextImpl enterConstant(Class<?> owner, Object self, Object[] args, MethodHandle beforeChain) throws Throwable {
        AdviceContextImpl context = new AdviceContextImpl(owner, self, args);
        beforeChain.invokeExact(context);
        return context;
    }

    /** Constant dispatch twin of enter for sites without before patches, only creates the context. */
    public static AdviceContextImpl createContext(Class<?> owner, Object self, Object[] args) {
        return new AdviceContextImpl(owner, self, args);
    }

    public static Object exit(int siteId, AdviceContextImpl context, Object returned) {
        PatchSite site = PatchRegistry.site(siteId);

        //If a before handler skipped a method, set the return value it left behind as the current return value.
        if (!context.isSkipOriginal()) {
            context.setReturnValue(returned);
        }

        for (PatchHandler patch : site.afterPatches()) {
            invoke(patch, context);
        }
        return context.getReturnValue();
    }

    /** Constant dispatch twin of exit. */
    public static Object exitConstant(AdviceContextImpl context, Object returned, MethodHandle afterChain) throws Throwable {
        //If a before handler skipped a method, set the return value it left behind as the current return value.
        if (!context.isSkipOriginal()) {
            context.setReturnValue(returned);
        }

        afterChain.invokeExact(context);
        return context.getReturnValue();
    }

    public static Throwable except(int siteId, AdviceContextImpl context, Throwable thrown) {
        PatchSite site = PatchRegistry.site(siteId);
        context.initThrown(thrown);
        for (PatchHandler patch : site.exceptPatches()) {
            invoke(patch, context);
        }
        return context.getThrown();
    }

    /** Constant dispatch twin of except. */
    public static Throwable exceptConstant(AdviceContextImpl context, Throwable thrown, MethodHandle exceptChain) throws Throwable {
        context.initThrown(thrown);
        exceptChain.invokeExact(context);
        return context.getThrown();
    }

    private static void invoke(PatchHandler patch, AdviceContextImpl context) {
        try {
            patch.handler().invokeExact(context);
        } catch (Throwable ex) {
            PatchLibLogger.error("Ran in to an error while dispatcher was executing "
                    + patch.spec().handlerClass() + "#" + patch.spec().handlerMethod() + " from mod " + patch.spec().sourceMod().getId());
            throw uncheckedThrow(ex);
        }
    }

    /** Wraps an intercepted access in its priority-ordered layers, shared by all redirect kinds. The bridges normalize
     * the kind specific values before calling this: target is the call receiver or field owner, or null for a
     * construction; args are the call arguments, [value] for a write, or empty for a read. The result of a write is
     * unused. layers[0] (lowest priority) is the outermost and runs first; each layer reaches the next through
     * ctx.call(), and the innermost reaches realAccess. */
    public static Object redirect(int siteId, Class<?> hostOwner, MethodHandle original,
                                  Object target, Object[] args, Object hostSelf, Object[] hostArgs) throws Throwable {
        RedirectSite site = PatchRegistry.redirectSite(siteId);
        RealAccess realAccess = site.realAccess(original, args.length);
        PatchHandler[] layers = site.layers();
        if (layers.length == 0) return realAccess.call(target, args);
        return runLayer(layers, 0, hostOwner, hostSelf, hostArgs, target, args, realAccess);
    }

    /** Runs the layer at index with a fresh context. The context's call()/read()/write() comes back through here for
     * the layer below, or reaches realAccess past the last one. Called by redirect and RedirectContextImpl.proceed only. */
    public static Object runLayer(PatchHandler[] layers, int index, Class<?> hostOwner, Object hostSelf, Object[] hostArgs,
                                  Object target, Object[] args, RealAccess realAccess) {
        RedirectContextImpl ctx = new RedirectContextImpl(hostOwner, hostSelf, hostArgs, target, args, layers, index, realAccess);
        invokeLayer(layers[index], ctx);
        return ctx.getResult();
    }

    /** Runs one redirect layer. Mirrors invoke() for advice, but only blames this layer when the layer itself threw.
     * Exceptions surfacing from ctx.call() (the original access or a deeper layer) come back through proceed(), which
     * records them on the context, so they keep propagating without being double-logged or pinned on the wrong mod. */
    private static void invokeLayer(PatchHandler layer, RedirectContextImpl ctx) {
        try {
            layer.handler().invokeExact(ctx);
        } catch (Throwable ex) {
            if (!ctx.isPropagating(ex)) {
                PatchLibLogger.error("Ran in to an error while dispatcher was executing "
                        + layer.spec().handlerClass() + "#" + layer.spec().handlerMethod() + " from mod " + layer.spec().sourceMod().getId());
            }
            throw uncheckedThrow(ex);
        }
    }

    /**Throws an exception upwards without checking it on this level */
    @SuppressWarnings("unchecked")
    public static <T extends Throwable> RuntimeException uncheckedThrow(Throwable ex) throws T {
        throw (T) ex;
    }
}
