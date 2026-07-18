package patchlib.agent;

import java.lang.instrument.Instrumentation;

public class AgentMain {

    private static Instrumentation instrumentation;

    /** Called if "selfAttachAgent" succeeded from PatchLibModplugin. */
    public static void agentmain(String args, Instrumentation instrumentation) {
        AgentMain.instrumentation = instrumentation;
        System.setProperty("PatchLib_AgentAttached", "true");
    }

    /** Called if the -javaagent flag is set in the games vmparams.
     * This is a fallback for 32bit windows mostly, in which Alex decided to not ship the JRE with the attach module. */
    public static void premain(String args, Instrumentation instrumentation) {
        AgentMain.instrumentation = instrumentation;
        System.setProperty("PatchLib_AgentAttached", "true");
    }

    public static void init(ClassLoader modClassLoader) {


        //Discovery

        //Scan

        //Patch

        //Preload

        /* Notes:
         * 1. Create TypePool
         * 2. Start mod jar scanning for annotations
         * 3. Create the fast index for the gate matcher, also adding more things to evaluate with it than the original version for better performance
         * 4. Patch Install
         * 4.1 Advice Install
         * 4.2 Redirect Install
         * 5. Preload Starsector classes
         */
    }

}
