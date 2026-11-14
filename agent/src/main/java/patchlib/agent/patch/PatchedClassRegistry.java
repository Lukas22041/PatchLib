package patchlib.agent.patch;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Tracks which patches were installed in to which method, so that logged exceptions can point out potentially involved patches. */
public class PatchedClassRegistry {

    /** Class name -> method name -> patches. Methods are tracked by name only, as stack traces do not contain descriptors. Overloads share an entry because of that. */
    private static final Map<String, Map<String, Set<InstallationData>>> PATCHED_METHODS = new HashMap<>();

    /** Synchronised since class loading can happen synchronously, so to avoid a race condition.
     * @param methodName The internal name of the patched method, so constructors are {@code <init>}, which is also what stack traces use. */
    public static synchronized void register(String className, String methodName, List<InstallationData> installationDataList) {
        PATCHED_METHODS.computeIfAbsent(className, key -> new HashMap<>())
                .computeIfAbsent(methodName, key -> new LinkedHashSet<>())
                .addAll(installationDataList);
    }

    public static synchronized Set<InstallationData> getPatches(String className, String methodName) {
        Map<String, Set<InstallationData>> methods = PATCHED_METHODS.get(className);
        if (methods == null) return Set.of();
        Set<InstallationData> patches = methods.get(methodName);
        if (patches == null) return Set.of();
        return new LinkedHashSet<>(patches);
    }

}
