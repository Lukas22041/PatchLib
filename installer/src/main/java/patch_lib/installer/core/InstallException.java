package patch_lib.installer.core;

/** Thrown when an install step cannot complete. The message is meant to be shown to the user. */
public class InstallException extends Exception {

    public InstallException(String message) {
        super(message);
    }

    public InstallException(String message, Throwable cause) {
        super(message, cause);
    }
}
