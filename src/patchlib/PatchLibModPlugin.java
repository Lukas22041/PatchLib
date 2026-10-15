package patchlib;

import com.fs.starfarer.api.BaseModPlugin;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.ModSpecAPI;
import com.sun.tools.attach.AgentInitializationException;
import com.sun.tools.attach.AgentLoadException;
import com.sun.tools.attach.AttachNotSupportedException;
import com.sun.tools.attach.VirtualMachine;
import org.apache.log4j.Logger;
import patchlib.agent.AgentMain;
import patchlib.test.PatchLibTests;

import javax.management.ObjectName;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.nio.file.Path;
import java.nio.file.Paths;

public class PatchLibModPlugin extends BaseModPlugin {

    private static final Logger log = Global.getLogger(PatchLibModPlugin.class);

    @Override
    public void onApplicationLoad() throws Exception {

        if (!checkAlreadyAttached()) {
            try {
                selfAttachAgent();
            } catch (Throwable attachEx) {
                //Self-attach can throw after agentmain already ran, in which case loading it a second time is not needed.
                if (!checkAlreadyAttached()) {
                    log.warn("PatchLib: Self-attach failed (" + attachEx + "). Falling back to loading the agent through the JVMs diagnostic command.");
                    try {
                        loadAgentThroughDiagnosticCommand();
                        log.info("PatchLib: Loaded the agent through the diagnostic command fallback.");
                    } catch (Throwable fallbackEx) {
                        //Both attempts failed. The fallbacks error is the cause, the self-attach error is kept as suppressed so both show up in the log.
                        RuntimeException ex = new RuntimeException("PatchLib failed to attach to the game. \n" +
                                "Check PatchLib's forum thread for guidance on this issue.\n", fallbackEx);
                        ex.addSuppressed(attachEx);
                        throw ex;
                    }
                }
            }
        }

        //Init the agent if it managed to attach.
        //Pass the mod classloader, as it is needed to bind the patches method handlers later.
        AgentMain.init(this.getClass().getClassLoader());

        PatchLibTests patchLibTests = new PatchLibTests();
        patchLibTests.runTests();
    }

    @Override
    public void onGameLoad(boolean newGame) {
        AgentMain.releaseScanData();
    }

    /** Check if the agent was already attached by a -javaagent flag in the vmparams.
     * Can't just check a static check, as*/
    public boolean checkAlreadyAttached() {
        String isAttached = System.getProperty("PatchLib_AgentAttached");
        return isAttached != null;
    }

    /** Self-attaches PatchLib's agent to the game.
     * Should be stable, as long as:
     * 1. The game ships with the "jdk.attach" module included in the bundled JRE/JDK.
     * 2. The game has the "-Djdk.attach.allowAttachSelf=true -XX:+EnableDynamicAgentLoading" flags set in the vmparams.
     * Alex has confirmed that both of those will happen with v0.98.5a
     * */
    public void selfAttachAgent() throws AgentLoadException, IOException, AgentInitializationException, AttachNotSupportedException {
        String agentJar = relativeToWorkingDir(getAgentJarPath());

        String pid = Long.toString(ProcessHandle.current().pid());

        VirtualMachine vm = VirtualMachine.attach(pid);
        vm.loadAgent(agentJar);
        vm.detach();
    }

    /** Fallback for when self-attach is not available, i.e. if the bundled JRE lacks the "jdk.attach" module.
     * Invokes HotSpots "JVMTI.agent_load" diagnostic command on the running JVM, which loads a .jar as a java agent
     * and calls its agentmain before returning. */
    public void loadAgentThroughDiagnosticCommand() throws Exception {
        String agentJar = relativeToWorkingDir(getAgentJarPath());

        Object output = ManagementFactory.getPlatformMBeanServer().invoke(
                new ObjectName("com.sun.management:type=DiagnosticCommand"),
                "jvmtiAgentLoad",
                new Object[]{ new String[]{ "\"" + agentJar + "\"" } },
                new String[]{ String[].class.getName() });

        //The command reports some failures through its output instead of an exception, so confirm that agentmain ran.
        if (!checkAlreadyAttached()) {
            throw new IllegalStateException("The diagnostic command did not load the agent. Command output: " + String.valueOf(output).trim());
        }
    }

    /** Returns the path relative to the working directory, or the path unchanged if that is not possible (i.e. different drives).
     * Both agent loading paths fail on absolute Windows paths with non-ASCII characters, but accept relative ones. */
    private String relativeToWorkingDir(String path) {
        try {
            Path workingDir = Paths.get(System.getProperty("user.dir")).toAbsolutePath().normalize();
            return workingDir.relativize(Paths.get(path).toAbsolutePath().normalize()).toString();
        } catch (Exception e) {
            return path;
        }
    }

    private String getAgentJarPath() {
        ModSpecAPI modSpec = Global.getSettings().getModManager().getModSpec("patchlib");
        return modSpec.getPath() + "/jars/PatchLibAgent.jar";
    }



}
