package patch_lib.agent;

import java.lang.instrument.Instrumentation;

public class PatchLibAgentInit {

    private static PatchLibAgentInit instance;
    private Instrumentation instrumentation;

    private PatchLibAgentInit() { }

    private PatchLibAgentInit(Instrumentation instrumentation) {
        this.instrumentation = instrumentation;
    }

    public static void createInstance(Instrumentation instrumentation) {
        instance = new PatchLibAgentInit(instrumentation);
    }

    public static void init() {

    }

}
