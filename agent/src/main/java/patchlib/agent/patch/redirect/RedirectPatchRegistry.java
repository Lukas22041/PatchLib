package patchlib.agent.patch.redirect;

import net.bytebuddy.description.ByteCodeElement;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

public class RedirectPatchRegistry {

    private static final CopyOnWriteArrayList<RedirectPatchSite> REDIRECT_PATCH_SITES = new CopyOnWriteArrayList<>();
    private static final Map<String, Integer> REDIRECT_PATCH_SITE_IDS = new HashMap<>();

    /** Synchronised since class loading can happen synchronously, so to avoid a race condition. */
    public static synchronized int register(String siteKey, RedirectPatchSite site) {

        Integer existing = REDIRECT_PATCH_SITE_IDS.get(siteKey);
        if (existing != null) return existing;

        REDIRECT_PATCH_SITES.add(site);
        int patchId = REDIRECT_PATCH_SITES.size() - 1;
        REDIRECT_PATCH_SITE_IDS.put(siteKey, patchId);

        return patchId;
    }

    public static String getSiteKey(String methodKey, String kind, ByteCodeElement.Member member) {
        return methodKey + " - " + kind + " - " + getMemberKey(member);
    }

    public static String getMemberKey(ByteCodeElement.Member member) {
        return member.getDeclaringType().asErasure().getName() + ":" + member.getInternalName() + ":" + member.getDescriptor();
    }

}
