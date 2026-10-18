package patchlib.agent.patch;

import patchlib.agent.log.PatchLibLogger;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

/** Holds the activation state for patches.
 * Patches themselves hold the same state in their activation data, so if its toggled it changes their reference as well */
public class PatchStateRegistry {

    // Patch Class Name / State
    private static final Map<String, AtomicBoolean> PATCH_STATES = new HashMap<>();

    public static synchronized AtomicBoolean getOrCreate(String patchClassName) {
        return PATCH_STATES.computeIfAbsent(patchClassName, key -> new AtomicBoolean(true));
    }

    public static synchronized boolean isEnabled(String patchClassName) {
        AtomicBoolean state = PATCH_STATES.get(patchClassName);
        return state == null || state.get();
    }

    public static synchronized void setEnabled(String patchClassName, boolean enabled) {
        AtomicBoolean state = getOrCreate(patchClassName);
        if (state.get() == enabled) return;

        state.set(enabled);
        PatchLibLogger.info((enabled ? "Enabled" : "Disabled") + " the patch " + patchClassName);
    }

}
