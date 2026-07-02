package patchlib.agent.dispatch;

import patchlib.agent.Patch;
import patchlib.agent.PatchLibLogger;
import patchlib.agent.PatchRegistry;
import patchlib.agent.PatchSite;
import patchlib.agent.RedirectSite;
import patchlib.agent.context.PatchContext;
import patchlib.agent.context.RedirectContextImpl;

import java.lang.invoke.MethodHandle;

/** Class that handles dispatching patches. PatchInstaller inserts bytecode in to every patched class,
 * which uses this dispatcher to delegate work to the patch handlers. */
public class PatchDispatcher {

    static final Object[] NO_ARGS = new Object[0];

    public static PatchContext enter(int siteId, Class<?> owner, Object self, Object[] args) {
        PatchSite site = PatchRegistry.site(siteId);
        PatchContext context = new PatchContext(owner, self, args);
        for (Patch patch : site.beforePatches()) {
            invoke(patch, context);
        }
        return context;
    }

    public static Object exit(int siteId, PatchContext context, Object returned) {
        PatchSite site = PatchRegistry.site(siteId);

        //If a before handler skipped a method, set the return value it left behind as the current return value.
        if (!context.isSkipOriginal()) {
            context.setReturnValue(returned);
        }

        for (Patch patch : site.afterPatches()) {
            invoke(patch, context);
        }
        return context.getReturnValue();
    }

    public static Throwable except(int siteId, PatchContext context, Throwable thrown) {
        PatchSite site = PatchRegistry.site(siteId);
        context.initThrown(thrown);
        for (Patch patch : site.exceptPatches()) {
            invoke(patch, context);
        }
        return context.getThrown();
    }

    private static void invoke(Patch patch, PatchContext context) {
        try {
            patch.handler().invokeExact(context);
        } catch (Throwable ex) {
            PatchLibLogger.error("Ran in to an error while dispatcher was executing "
                    + patch.spec().handlerClass() + "#" + patch.spec().handlerMethod() + " from mod " + patch.spec().sourceMod().getId());
            uncheckedThrow(ex);
        }
    }

    /** Wraps an intercepted method call in its layers. The single argument is the call receiver (absent for a static
     * call), followed by the call arguments. */
    public static Object redirectMethodCall(int siteId, Class<?> hostOwner, MethodHandle original,
                                            Object callReceiver, Object[] callArgs, Object hostSelf, Object[] hostArgs) throws Throwable {
        RedirectSite site = PatchRegistry.redirectSite(siteId);
        MethodHandle spread = site.spreadOriginal(original);
        boolean hasReceiver = original.type().parameterCount() > callArgs.length;
        Operation realCall = args -> spread.invokeExact(hasReceiver ? prepend(callReceiver, args) : args);
        return wrap(site.layers(), hostOwner, hostSelf, hostArgs, callReceiver, callArgs, realCall);
    }

    /** Wraps an intercepted constructor call in its layers. There is no receiver, the original handle allocates and
     * initializes in one step. */
    public static Object redirectConstructorCall(int siteId, Class<?> hostOwner, MethodHandle original,
                                                 Object[] callArgs, Object hostSelf, Object[] hostArgs) throws Throwable {
        RedirectSite site = PatchRegistry.redirectSite(siteId);
        MethodHandle spread = site.spreadOriginal(original);
        Operation realNew = args -> spread.invokeExact(args);
        return wrap(site.layers(), hostOwner, hostSelf, hostArgs, null, callArgs, realNew);
    }

    /** Wraps an intercepted field read in its layers. A read takes no arguments. */
    public static Object redirectFieldRead(int siteId, Class<?> hostOwner, MethodHandle original,
                                           Object fieldOwner, Object hostSelf, Object[] hostArgs) throws Throwable {
        RedirectSite site = PatchRegistry.redirectSite(siteId);
        MethodHandle spread = site.spreadOriginal(original);
        boolean hasReceiver = original.type().parameterCount() > 0;
        Operation realRead = args -> spread.invokeExact(hasReceiver ? new Object[]{fieldOwner} : NO_ARGS);
        return wrap(site.layers(), hostOwner, hostSelf, hostArgs, fieldOwner, NO_ARGS, realRead);
    }

    /** Wraps an intercepted field write in its layers. The single argument is the value being written. */
    public static void redirectFieldWrite(int siteId, Class<?> hostOwner, MethodHandle original,
                                          Object fieldOwner, Object value, Object hostSelf, Object[] hostArgs) throws Throwable {
        RedirectSite site = PatchRegistry.redirectSite(siteId);
        MethodHandle spread = site.spreadOriginal(original);
        boolean hasReceiver = original.type().parameterCount() > 1;
        //A write has no result; the null that the adapted void return produces is discarded by wrap.
        Operation realWrite = args -> spread.invokeExact(hasReceiver ? new Object[]{fieldOwner, args[0]} : new Object[]{args[0]});
        wrap(site.layers(), hostOwner, hostSelf, hostArgs, fieldOwner, new Object[]{value}, realWrite);
    }

    /** Wraps an intercepted access in its priority-ordered layers, shared by all redirect kinds. layers[0]
     * (lowest priority) is the outermost and runs first; each layer reaches the next through ctx.call(), and the
     * innermost reaches realAccess. target is the call receiver or field owner, or null for a construction; startArgs
     * are the call arguments, [value] for a write, or NO_ARGS for a read. The result of a write is unused. */
    private static Object wrap(Patch[] layers, Class<?> hostOwner, Object hostSelf, Object[] hostArgs,
                               Object target, Object[] startArgs, Operation realAccess) throws Throwable {
        if (layers.length == 0) return realAccess.call(startArgs);
        return runLayer(layers, 0, hostOwner, hostSelf, hostArgs, target, startArgs, realAccess);
    }

    /** Runs the layer at index with a fresh context. The context's call()/read()/write() comes back through here for
     * the layer below, or reaches realAccess past the last one. Called by wrap and RedirectContextImpl.proceed only. */
    public static Object runLayer(Patch[] layers, int index, Class<?> hostOwner, Object hostSelf, Object[] hostArgs,
                                  Object target, Object[] args, Operation realAccess) {
        RedirectContextImpl ctx = new RedirectContextImpl(hostOwner, hostSelf, hostArgs, target, args, layers, index, realAccess);
        invokeLayer(layers[index], ctx);
        return ctx.getResult();
    }

    /** Runs one redirect layer. Mirrors invoke() for advice, but only blames this layer when the layer itself threw.
     * Exceptions surfacing from ctx.call() (the original access or a deeper layer) come back through proceed(), which
     * records them on the context, so they keep propagating without being double-logged or pinned on the wrong mod. */
    private static void invokeLayer(Patch layer, RedirectContextImpl ctx) {
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

    private static Object[] prepend(Object head, Object[] tail) {
        Object[] full = new Object[tail.length + 1];
        full[0] = head;
        System.arraycopy(tail, 0, full, 1, tail.length);
        return full;
    }

    /**Throws an exception upwards without checking it on this level */
    @SuppressWarnings("unchecked")
    public static <T extends Throwable> RuntimeException uncheckedThrow(Throwable ex) throws T {
        throw (T) ex;
    }
}
