package patch_lib.agent;

import java.lang.instrument.Instrumentation;

/** The program entry for the agent.
 * This is called before the games launcher starts. There is not much to do yet at this stage,
 * as such the only thing the agent does is set a system property of its version to confirm its load and prepare the later init. */
public class PreMain {
    public static void premain(String args, Instrumentation instrumentation) {
        String version = PreMain.class.getPackage().getImplementationVersion();
        System.setProperty("patch_lib.agent.version", version);

        PatchLibAgentInit.createInstance(instrumentation); //Delegate the call until much later when onApplicationLoad is called from the mods side.
    }
}
