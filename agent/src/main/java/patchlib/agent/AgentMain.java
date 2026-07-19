package patchlib.agent;

import patchlib.agent.log.PatchlibLogger;
import patchlib.agent.misc.StarsectorPreloader;
import patchlib.agent.scan.ClassDiscoverer;
import patchlib.agent.scan.ClassScanner;
import patchlib.agent.scan.DiscoveryData;
import patchlib.api.PatchLib;
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

        PatchlibLogger.info("Starting initialization");
        long start = System.currentTimeMillis();

        //Discovery
        ClassDiscoverer discoverer = new ClassDiscoverer();
        DiscoveryData discoveryData = discoverer.discover();

        //Scan
        ClassScanner scanner = new ClassScanner(discoveryData);

        //API init
        PatchLibImpl.init(scanner);

        //PatchScanner

        //Patch

        //Preload
        ClassLoader systemLoader = AgentMain.class.getClassLoader();
        StarsectorPreloader preloader = new StarsectorPreloader(discoveryData, systemLoader);
        preloader.preload();

        float time = (System.currentTimeMillis() - start) / 1000f;
        PatchlibLogger.info("Finished initialization in " + time + " seconds");
    }

}
