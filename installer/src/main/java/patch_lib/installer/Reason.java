package patch_lib.installer;

/** Why the installer was opened. Drives the description shown to the user. */
public enum Reason {
    MISSING_AGENT,
    VERSION_MISMATCH;

    public static Reason fromName(String name) {
        if (name == null) {
            return MISSING_AGENT;
        }
        try {
            return Reason.valueOf(name);
        } catch (IllegalArgumentException e) {
            return MISSING_AGENT;
        }
    }
}
