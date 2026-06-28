package patchlib.installer.core;

/** Outcome of an install attempt. Shown to the user and carried back from the elevated process. */
public final class ApplyResult {

    public final boolean success;
    public final boolean cancelled;
    public final String message;

    private ApplyResult(boolean success, boolean cancelled, String message) {
        this.success = success;
        this.cancelled = cancelled;
        this.message = message;
    }

    public static ApplyResult ok(String message) {
        return new ApplyResult(true, false, message);
    }

    public static ApplyResult failure(String message) {
        return new ApplyResult(false, false, message);
    }

    public static ApplyResult cancelled() {
        return new ApplyResult(false, true, "Install cancelled. Nothing was changed.");
    }
}
