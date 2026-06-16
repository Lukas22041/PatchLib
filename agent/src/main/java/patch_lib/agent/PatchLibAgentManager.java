package patch_lib.agent;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import patch_lib.agent.discover.PatchScanner;

import java.lang.instrument.Instrumentation;

public class PatchLibAgentManager {

    private static PatchLibAgentManager instance;
    private Instrumentation instrumentation;

    private PatchLibAgentManager() { }

    private PatchLibAgentManager(Instrumentation instrumentation) {
        this.instrumentation = instrumentation;
    }

    public static void createInstance(Instrumentation instrumentation) {
        instance = new PatchLibAgentManager(instrumentation);
    }

    public static PatchLibAgentManager getInstance() { return instance; }

    public void init() {
        PatchLibLogger.debug("PatchLib agent started.");

        PatchScanner scanner = new PatchScanner();
        scanner.scan();
    }

}
