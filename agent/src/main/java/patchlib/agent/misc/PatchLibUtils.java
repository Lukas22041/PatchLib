package patchlib.agent.misc;

public class PatchLibUtils {

    /** How many threads to use for multithreaded work. Defaults to a maximum of 8 and minimum of 1, and otherwise always 1 thread less than available */
    public static int getAvailableThreads() {
        return Math.max(1, Math.min(8, Runtime.getRuntime().availableProcessors()-1));
    }

}
