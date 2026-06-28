package patchlib.agent;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

public class PatchLibLogger {

    private static final Logger log = Logger.getLogger(PatchLibLogger.class);

    static {
        log.setLevel(Level.ALL);
    }

    public static void info(String message) {
        log.info("[PatchLib] " + message);
    }

    public static void debug(String message) {
        log.debug("[PatchLib] " + message);
    }

    public static void warn(String message) {
        log.warn("[PatchLib] " + message);
    }

    public static void error(String message) {
        log.error("[PatchLib] " + message);
    }
}
