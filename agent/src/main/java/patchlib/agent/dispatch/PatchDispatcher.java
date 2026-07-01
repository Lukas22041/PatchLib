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

    /** Holds the exception currently unwinding through a redirect chain, so an enclosing layer frame can tell a
     * propagated exception (surfaced from ctx.call()) apart from one the layer itself threw. See invokeLayer. */
    private static final ThreadLocal<Throwable> PROPAGATING = new ThreadLocal<>();

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
        boolean hasReceiver = original.type().parameterCount() > callArgs.length;
        Operation realCall = args -> original.invokeWithArguments(hasReceiver ? prepend(callReceiver, args) : args);
        return wrap(site.layers(), hostOwner, hostSelf, hostArgs, callReceiver, callArgs, realCall);
    }

    /** Wraps an intercepted field read in its layers. A read takes no arguments. */
    public static Object redirectFieldRead(int siteId, Class<?> hostOwner, MethodHandle original,
                                           Object fieldOwner, Object hostSelf, Object[] hostArgs) throws Throwable {
        RedirectSite site = PatchRegistry.redirectSite(siteId);
        boolean hasReceiver = original.type().parameterCount() > 0;
        Operation realRead = args -> original.invokeWithArguments(hasReceiver ? new Object[]{fieldOwner} : NO_ARGS);
        return wrap(site.layers(), hostOwner, hostSelf, hostArgs, fieldOwner, NO_ARGS, realRead);
    }

    /** Wraps an intercepted field write in its layers. The single argument is the value being written. */
    public static void redirectFieldWrite(int siteId, Class<?> hostOwner, MethodHandle original,
                                          Object fieldOwner, Object value, Object hostSelf, Object[] hostArgs) throws Throwable {
        RedirectSite site = PatchRegistry.redirectSite(siteId);
        boolean hasReceiver = original.type().parameterCount() > 1;
        Operation realWrite = args -> {
            original.invokeWithArguments(hasReceiver ? new Object[]{fieldOwner, args[0]} : new Object[]{args[0]});
            return null;
        };
        wrap(site.layers(), hostOwner, hostSelf, hostArgs, fieldOwner, new Object[]{value}, realWrite);
    }

    /** Wraps an intercepted access in its priority-ordered layers, shared by all three redirect kinds. layers[0]
     * (lowest priority) is the outermost and runs first; each layer reaches the next through ctx.call(), and the
     * innermost reaches realAccess. target is the call receiver or field owner; startArgs are the call arguments,
     * [value] for a write, or NO_ARGS for a read. The result of a write is unused. */
    private static Object wrap(Patch[] layers, Class<?> hostOwner, Object hostSelf, Object[] hostArgs,
                               Object target, Object[] startArgs, Operation realAccess) throws Throwable {
        Operation op = realAccess;
        for (int i = layers.length - 1; i >= 0; i--) {
            Operation next = op;
            Patch layer = layers[i];
            op = args -> {
                RedirectContextImpl ctx = new RedirectContextImpl(hostOwner, hostSelf, hostArgs, target, args, next);
                invokeLayer(layer, ctx);
                return ctx.getResult();
            };
        }
        return op.call(startArgs);
    }

    /** Runs one redirect layer. Mirrors invoke() for advice, but only blames this layer when the layer itself threw.
     * Exceptions surfacing from ctx.call() (the original access or a deeper layer) come back through proceed(), which
     * records them in PROPAGATING, so they keep propagating without being double-logged or pinned on the wrong mod. */
    private static void invokeLayer(Patch layer, RedirectContextImpl ctx) {
        try {
            layer.handler().invokeExact(ctx);
        } catch (Throwable ex) {
            if (PROPAGATING.get() != ex) {
                PatchLibLogger.error("Ran in to an error while dispatcher was executing "
                        + layer.spec().handlerClass() + "#" + layer.spec().handlerMethod() + " from mod " + layer.spec().sourceMod().getId());
            }
            throw uncheckedThrow(ex);
        }
    }

    /** Records t as propagating up through the redirect chain (see invokeLayer), then rethrows it unchecked.
     * Called from RedirectContextImpl.proceed when a deeper layer or the original access throws. */
    public static RuntimeException propagate(Throwable t) {
        PROPAGATING.set(t);
        return uncheckedThrow(t); //Always throws; the return only satisfies the compiler at the call site.
    }

    private static Object[] prepend(Object head, Object[] tail) {
        Object[] full = new Object[tail.length + 1];
        full[0] = head;
        System.arraycopy(tail, 0, full, 1, tail.length);
        return full;
    }

    /**Throws an exception upwards without checking it on this level */
    @SuppressWarnings("unchecked")
    static <T extends Throwable> RuntimeException uncheckedThrow(Throwable ex) throws T {
        throw (T) ex;
    }
}
