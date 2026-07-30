package patchlib.agent.patch.redirect;

import patchlib.agent.log.PatchLibLogger;
import patchlib.agent.spec.PatchHandlerSpec;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Method;

public class RedirectBootstrap {

    public final static Method BOOTSTRAP_METHOD;
    static {
        try {
            BOOTSTRAP_METHOD = RedirectBootstrap.class.getMethod("bootstrap", MethodHandles.Lookup.class, String.class, Class.class, int.class, MethodHandle.class, int.class);
        } catch (NoSuchMethodException ex) {
            throw new RuntimeException("Failed to resolve bootstrap method", ex);
        }
    }

    public static MethodHandle bootstrap(MethodHandles.Lookup lookup, String name, Class<?> type, int siteId, MethodHandle original, int hasReceiver) {
        Class<?> hostClass = lookup.lookupClass();
        RedirectPatchSite site = RedirectPatchRegistry.getSite(siteId);
        PatchHandlerSpec.RedirectType redirectType = site.redirectType();

        MethodHandle converted = RedirectHandleChain.convertOriginalToChainHandle(redirectType, original, hasReceiver == 1);
        try {
            return RedirectHandleChain.wrapLayers(site, converted, hostClass);
        } catch (Throwable ex) {
            PatchLibLogger.error("Failed to create the redirect chain " + name + " for site " + siteId + ", the patches are not applied. (" + site + ")" + " " + "\n" + ex);
            return converted;
        }

    }


}
