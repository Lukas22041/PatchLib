package patchlib.agent.patch.holder;

import patchlib.agent.log.PatchLibLogger;
import patchlib.agent.patch.holder.HolderClassGenerator.HolderField;
import patchlib.agent.patch.redirect.RedirectHandleChain;

import java.lang.invoke.MethodHandle;
import java.lang.reflect.Field;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** The holders of redirect sites. Each holds the chain of one site, see {@link HolderClassGenerator}. */
public class RedirectHolders {

    private static final HolderField REDIRECT = HolderField.of("REDIRECT", RedirectHolders.class, "provideRedirect");

    //Site ids are reused when a class is transformed again, so the holders are as well.
    private static final Map<Integer, Class<?>> HOLDERS = new ConcurrentHashMap<>();


    //Used by the installer, to bind the field in to the patched code. The site has to be registered already.

    public static Field getRedirectField(int siteId) {
        return HolderClassGenerator.getField(getHolder(siteId), REDIRECT);
    }

    private static Class<?> getHolder(int siteId) {
        return HOLDERS.computeIfAbsent(siteId, RedirectHolders::generateHolder);
    }

    private static Class<?> generateHolder(int siteId) {
        return HolderClassGenerator.generate("RedirectHolder" + siteId, siteId, REDIRECT);
    }


    //Called by the generated holders to assign their field, when the redirected code runs for the first time.
    //Unlike for hooks there is nothing to fall back to on failure, as the chain also contains the code that the redirect replaced.

    public static MethodHandle provideRedirect(int siteId) throws Throwable {
        try {
            return RedirectHandleChain.createHandleChain(siteId);
        } catch (Throwable ex) {
            PatchLibLogger.error("Failed to provide the redirect chain for site " + siteId + ", the redirected code can not run", ex);
            throw ex;
        }
    }

}
