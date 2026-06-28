package patchlib.agent;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

/** Registers and remembers patch sites (i.e, patched methods). Returns the index on register even if already registered*/
public final class PatchRegistry {

    private PatchRegistry() { }

    private static final CopyOnWriteArrayList<PatchSite> SITES = new CopyOnWriteArrayList<>();
    private static final Map<String, Integer> IDS = new HashMap<>();

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

    public static PatchSite site(int id) { return SITES.get(id); }
}
