package patchlib.agent.patch.advice;

import patchlib.agent.log.PatchLibLogger;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Method;

public class AdviceBootstrap {

    public final static String BEFORE = "BEFORE";
    public final static String AFTER = "AFTER";

    public final static Method BOOTSTRAP_METHOD;
    static {
        try {
            BOOTSTRAP_METHOD = AdviceBootstrap.class.getMethod("bootstrap", MethodHandles.Lookup.class, String.class, Class.class, int.class);
        } catch (NoSuchMethodException ex) {
            throw new RuntimeException("Failed to resolve bootstrap method", ex);
        }
    }

    public static MethodHandle bootstrap(MethodHandles.Lookup lookup, String name, Class<?> type, int siteId) {
        try {
            AdvicePatchSite patchSite = AdvicePatchRegistry.getSite(siteId);
            if (name.equals(BEFORE)) return patchSite.beforeChain;
            else if (name.equals(AFTER)) return patchSite.afterChain;
            else throw new IllegalArgumentException("Could not find matching method handle for \"" + name + "\"");
        } catch (Throwable ex) {
            PatchLibLogger.error("Failed to bootstrap the " + name + " method handle for site " + siteId + ", its patches will be skipped");
            return MethodHandles.empty(AdviceHandleChain.handleChainType);
        }
    }


}
