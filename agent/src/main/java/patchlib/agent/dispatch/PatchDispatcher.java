package patchlib.agent.dispatch;

import patchlib.agent.PatchHandler;
import patchlib.agent.PatchLibLogger;
import patchlib.agent.PatchRegistry;
import patchlib.agent.PatchSite;
import patchlib.agent.context.AdviceContextImpl;

import java.lang.invoke.MethodHandle;

/** Class that handles dispatching advice patches. PatchInstaller inserts bytecode in to every patched class,
 * which uses this dispatcher to delegate work to the patch handlers. Redirect dispatch lives entirely in the
 * chains composed by ChainBootstrap. */
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

    /**Throws an exception upwards without checking it on this level */
    @SuppressWarnings("unchecked")
    public static <T extends Throwable> RuntimeException uncheckedThrow(Throwable ex) throws T {
        throw (T) ex;
    }
}
