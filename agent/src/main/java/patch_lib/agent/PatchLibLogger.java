package patch_lib.agent;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

public class PatchLibLogger {

    private static final Logger log = Logger.getLogger(PatchLibLogger.class);

    static {
        log.setLevel(Level.ALL);
    }

    public static void debug(String message) {
        log.debug("[PatchLib] " + message);
    }
}
