package patchlib.agent.log;

import com.fs.starfarer.api.Global;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

public class PatchlibLogger {


    private static Logger log = Global.getLogger(PatchlibLogger.class);
    private static String prefix = "[PatchLib] ";

    static {
        log.setLevel(Level.ALL);
    }

    public static void info(String message) {
        log.info(prefix + message);
    }

    public static void debug(String message) {
        log.debug(prefix + message);
    }

    public static void warn(String message) {
        log.warn(prefix + message);
    }

    public static void error(String message) {
        log.error(prefix + message);
    }

    public static void blank() {
        log.info("");
    }

}
