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
     * This is a fallback for 32bit windows mostly, in which Alex decided to not ship the JRE with the attach module.
     * Might be worth to attempt using ByteBuddys installer fallback instead*/
    public static void premain(String args, Instrumentation instrumentation) {
        AgentMain.instrumentation = instrumentation;
        System.setProperty("PatchLib_AgentAttached", "true");
    }

    public static void init(ClassLoader modClassLoader) {

        //Discovery

        //Scan

        //Patch

        //Preload

    }

}
