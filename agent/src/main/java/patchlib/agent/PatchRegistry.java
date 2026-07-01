package patchlib.agent;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

/** Registers and remembers patch sites (i.e, patched methods). Returns the index on register even if already registered*/
public final class PatchRegistry {

    private PatchRegistry() { }

    private static final CopyOnWriteArrayList<PatchSite> SITES = new CopyOnWriteArrayList<>();
    private static final Map<String, Integer> IDS = new HashMap<>();

    private static final CopyOnWriteArrayList<RedirectSite> REDIRECT_SITES = new CopyOnWriteArrayList<>();
    private static final Map<String, Integer> REDIRECT_IDS = new HashMap<>();

    public static int register(String methodKey, PatchSite site) {
        synchronized (SITES) {
            Integer existing = IDS.get(methodKey);
            if (existing != null) return existing;
            SITES.add(site);
            int id = SITES.size() - 1;
            IDS.put(methodKey, id);
            return id;
        }
    }

    public static PatchSite site(int id) {
        return SITES.get(id);
    }

    /** Redirect sites use a separate id space from the method sites above. A redirect bridge only ever resolves through redirectSite. */
    public static int registerRedirect(String callKey, RedirectSite site) {
        synchronized (REDIRECT_SITES) {
            Integer existing = REDIRECT_IDS.get(callKey);
            if (existing != null) return existing;
            REDIRECT_SITES.add(site);
            int id = REDIRECT_SITES.size() - 1;
            REDIRECT_IDS.put(callKey, id);
            return id;
        }
    }

    public static RedirectSite redirectSite(int id) {
        return REDIRECT_SITES.get(id);
    }
}
