package patchlib.agent.patch;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Tracks which patches were installed in to which class, so that logged exceptions can point out potentially involved patches. */
public class PatchedClassRegistry {

    private static final Map<String, Set<InstallationData>> PATCHED_CLASSES = new HashMap<>();

    /** Synchronised since class loading can happen synchronously, so to avoid a race condition. */
    public static synchronized void register(String className, List<InstallationData> installationDataList) {
        PATCHED_CLASSES.computeIfAbsent(className, key -> new LinkedHashSet<>()).addAll(installationDataList);
    }

    public static synchronized Set<InstallationData> getPatches(String className) {
        Set<InstallationData> patches = PATCHED_CLASSES.get(className);
        if (patches == null) return Set.of();
        return new LinkedHashSet<>(patches);
    }

}
