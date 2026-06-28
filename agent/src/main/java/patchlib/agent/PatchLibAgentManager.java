package patchlib.agent;

import patchlib.agent.discover.PatchScanner;
import patchlib.agent.patch.PatchInstaller;
import patchlib.agent.patch.StarsectorPreloader;
import patchlib.agent.spec.PatchSpec;

import java.lang.instrument.Instrumentation;
import java.util.List;

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

    public void init(ClassLoader loader) {
        PatchLibLogger.info("PatchLib agent started.");

        PatchScanner scanner = new PatchScanner();
        List<PatchSpec> patchSpecs = scanner.scan();

        PatchInstaller.install(instrumentation, patchSpecs, loader);

        StarsectorPreloader.preload(loader);
    }

}
