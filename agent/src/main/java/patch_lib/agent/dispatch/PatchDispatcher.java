package patch_lib.agent.dispatch;

import patch_lib.agent.Patch;
import patch_lib.agent.PatchLibLogger;
import patch_lib.agent.PatchRegistry;
import patch_lib.agent.PatchSite;
import patch_lib.api.PatchContext;

/** Class that handles dispatching patches. PatchInstaller inserts bytecode in to every patched class,
 * which uses this dispatcher to delegate work to the patch handlers. */
public class PatchDispatcher {

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

    /**Throws an exception upwards without checking it on this level */
    @SuppressWarnings("unchecked")
    private static <T extends Throwable> void uncheckedThrow(Throwable ex) throws T {
        throw (T) ex;
    }
}
