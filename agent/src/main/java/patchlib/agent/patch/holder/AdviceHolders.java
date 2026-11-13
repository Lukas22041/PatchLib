package patchlib.agent.patch.holder;

import patchlib.agent.context.HookContextImpl;
import patchlib.agent.log.PatchLibLogger;
import patchlib.agent.patch.advice.AdvicePatchRegistry;
import patchlib.agent.patch.holder.HolderClassGenerator.HolderField;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Field;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** The holders of hook sites. Each holds the before and the after chain of one site, see {@link HolderClassGenerator}. */
public class AdviceHolders {

    private static final HolderField BEFORE = HolderField.of("BEFORE", AdviceHolders.class, "provideBefore");
    private static final HolderField AFTER = HolderField.of("AFTER", AdviceHolders.class, "provideAfter");

    //Site ids are reused when a class is transformed again, so the holders are as well.
    private static final Map<Integer, Class<?>> HOLDERS = new ConcurrentHashMap<>();

    private static final MethodHandle EMPTY_CHAIN = MethodHandles.empty(MethodType.methodType(void.class, HookContextImpl.class));


    //Used by the installer, to bind the fields in to the patched code. The site has to be registered already.

    public static Field getBeforeField(int siteId) {
        return HolderClassGenerator.getField(getHolder(siteId), BEFORE);
    }

    public static Field getAfterField(int siteId) {
        return HolderClassGenerator.getField(getHolder(siteId), AFTER);
    }

    private static Class<?> getHolder(int siteId) {
        return HOLDERS.computeIfAbsent(siteId, AdviceHolders::generateHolder);
    }

    private static Class<?> generateHolder(int siteId) {
        return HolderClassGenerator.generate("AdviceHolder" + siteId, siteId, BEFORE, AFTER);
    }


    //Called by the generated holders to assign their fields, when the patched code runs for the first time.
    //On failure the site gets an empty chain, which skips its patches but keeps the patched method working.

    public static MethodHandle provideBefore(int siteId) {
        try {
            return AdvicePatchRegistry.getSite(siteId).beforeChain;
        } catch (Throwable ex) {
            PatchLibLogger.error("Failed to provide the before chain for site " + siteId + ", its patches will be skipped", ex);
            return EMPTY_CHAIN;
        }
    }

    public static MethodHandle provideAfter(int siteId) {
        try {
            return AdvicePatchRegistry.getSite(siteId).afterChain;
        } catch (Throwable ex) {
            PatchLibLogger.error("Failed to provide the after chain for site " + siteId + ", its patches will be skipped", ex);
            return EMPTY_CHAIN;
        }
    }

}
