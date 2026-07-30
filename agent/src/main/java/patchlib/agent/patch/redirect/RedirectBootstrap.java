package patchlib.agent.patch.redirect;

import patchlib.agent.log.PatchLibLogger;
import patchlib.agent.patch.advice.AdviceHandleChain;
import patchlib.agent.patch.advice.AdvicePatchRegistry;
import patchlib.agent.patch.advice.AdvicePatchSite;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Method;

public class RedirectBootstrap {

    public final static String BEFORE = "BEFORE";
    public final static String AFTER = "AFTER";

    public final static Method BOOTSTRAP_METHOD;
    static {
        try {
            BOOTSTRAP_METHOD = RedirectBootstrap.class.getMethod("bootstrap", MethodHandles.Lookup.class, String.class, Class.class, int.class, Class.class, MethodHandle.class, int.class);
        } catch (NoSuchMethodException ex) {
            throw new RuntimeException("Failed to resolve bootstrap method", ex);
        }
    }

    public static MethodHandle bootstrap(MethodHandles.Lookup lookup, String name, Class<?> type, int siteId, Class<?> hostClass, MethodHandle original, int hasReceiver) {



    }


}
