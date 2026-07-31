package patchlib.agent.patch.advice;

import net.bytebuddy.description.method.MethodDescription;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

public class AdvicePatchRegistry {

    private static final CopyOnWriteArrayList<AdvicePatchSite> ADVICE_PATCH_SITES = new CopyOnWriteArrayList<>();
    private static final Map<String, Integer> ADVICE_PATCH_SITE_IDS = new HashMap<>();

    /** Synchronised since class loading can happen synchronously, so to avoid a race condition. */
    public static synchronized int register(String key, AdvicePatchSite site) {

        Integer existing = ADVICE_PATCH_SITE_IDS.get(key);
        if (existing != null) return existing;

        ADVICE_PATCH_SITES.add(site);
        int patchId = ADVICE_PATCH_SITES.size() - 1;
        ADVICE_PATCH_SITE_IDS.put(key, patchId);

        return patchId;
    }

    public static AdvicePatchSite getSite(int patchId) {
        return ADVICE_PATCH_SITES.get(patchId);
    }

    public static String getSiteKey(MethodDescription member) {
        return member.getDeclaringType().asErasure().getName() + ":" + member.getInternalName() + ":" + member.getDescriptor();
    }

}
