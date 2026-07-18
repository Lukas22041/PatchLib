package patchlib.agent;

import patchlib.agent.scan.ClassDiscoverer;
import patchlib.agent.scan.ClassScanner;
import patchlib.api.PatchLibImpl;
import patchlib.api.data.ClassData;

import java.lang.instrument.Instrumentation;
import java.util.List;

public class AgentMain {

    private final static String ATTACHED_PROPERTY = "PatchLib_AgentAttached";
    private static Instrumentation instrumentation;

    /** Called if "selfAttachAgent" succeeded from PatchLibModplugin. */
    public static void agentmain(String args, Instrumentation instrumentation) {
        AgentMain.instrumentation = instrumentation;
        System.setProperty(ATTACHED_PROPERTY, "true");
    }

    /** Called if the -javaagent flag is set in the games vmparams.
     * This is a fallback for 32bit windows mostly, in which Alex decided to not ship the JRE with the attach module.
     * Might be worth to attempt using ByteBuddys installer fallback instead */
    public static void premain(String args, Instrumentation instrumentation) {
        AgentMain.instrumentation = instrumentation;
        System.setProperty(ATTACHED_PROPERTY, "true");
    }

    public static void init(ClassLoader modClassLoader) {

        //Discovery

        ClassDiscoverer discoverer = new ClassDiscoverer();
        List<ClassData> classes = discoverer.discover();

        //Scan
        ClassScanner scanner = new ClassScanner(classes);

        //API init
        PatchLibImpl.init(scanner);

        //PatchScanner

        //Patch

        //Preload

    }

}
