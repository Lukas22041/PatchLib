package patchlib.agent;

import patchlib.agent.spec.PatchSpec;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/** Registers and remembers patch sites (i.e, patched methods). Returns the index on register even if already registered*/
public final class PatchRegistry {

    private PatchRegistry() { }

    private static final CopyOnWriteArrayList<PatchSite> SITES = new CopyOnWriteArrayList<>();
    private static final Map<String, Integer> IDS = new HashMap<>();

    private static final CopyOnWriteArrayList<RedirectSite> REDIRECT_SITES = new CopyOnWriteArrayList<>();
    private static final Map<String, Integer> REDIRECT_IDS = new HashMap<>();

    /** Every spec the scanner discovered, installed or not. Set once at init, used by diagnostics. */
    private static volatile List<PatchSpec> SCANNED = List.of();

    /** Supertypes (transitive superclasses and interfaces, minus Object) of each patched class, recorded as
     * the class is transformed. Only used by the debug menu's group-by-supertype view. */
    private static final Map<String, Set<String>> SUPERTYPES = new ConcurrentHashMap<>();

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

    /** Snapshot of all advice sites keyed by method key. For diagnostics, not dispatch. */
    public static Map<String, PatchSite> siteSnapshot() {
        synchronized (SITES) {
            Map<String, PatchSite> out = new HashMap<>();
            for (Map.Entry<String, Integer> entry : IDS.entrySet()) {
                out.put(entry.getKey(), SITES.get(entry.getValue()));
            }
            return out;
        }
    }

    /** Snapshot of all redirect sites keyed by call key. For diagnostics, not dispatch. */
    public static Map<String, RedirectSite> redirectSnapshot() {
        synchronized (REDIRECT_SITES) {
            Map<String, RedirectSite> out = new HashMap<>();
            for (Map.Entry<String, Integer> entry : REDIRECT_IDS.entrySet()) {
                out.put(entry.getKey(), REDIRECT_SITES.get(entry.getValue()));
            }
            return out;
        }
    }

    /** Records the full scanned spec list so diagnostics can tell which specs never installed. */
    public static void setScannedSpecs(List<PatchSpec> specs) {
        SCANNED = List.copyOf(specs);
    }

    public static List<PatchSpec> scannedSpecs() {
        return SCANNED;
    }

    /** Records a patched class's supertypes for the diagnostics view. Overwrites on re-transform, which is fine. */
    public static void recordSupertypes(String className, Set<String> supertypes) {
        SUPERTYPES.put(className, supertypes);
    }

    public static Map<String, Set<String>> supertypeSnapshot() {
        return new HashMap<>(SUPERTYPES);
    }
}
