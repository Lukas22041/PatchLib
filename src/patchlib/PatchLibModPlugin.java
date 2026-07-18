package patchlib;

import com.fs.starfarer.api.BaseModPlugin;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.ModSpecAPI;
import com.sun.tools.attach.AgentInitializationException;
import com.sun.tools.attach.AgentLoadException;
import com.sun.tools.attach.AttachNotSupportedException;
import com.sun.tools.attach.VirtualMachine;
import patchlib.agent.AgentMain;
import patchlib.api.PatchLib;
import patchlib.api.patch.Patch;
import patchlib.api.query.AnnotationQuery;
import patchlib.api.query.ClassQuery;

import java.io.IOException;

public class PatchLibModPlugin extends BaseModPlugin {

    @Override
    public void onApplicationLoad() throws Exception {

        if (!checkAlreadyAttached()) {
            selfAttachAgent();
        }

        //Init the agent if it managed to attach.
        //Pass the mod classloader, as it is needed to bind the patches method handlers later.
        AgentMain.init(this.getClass().getClassLoader());


        ClassQuery query = ClassQuery.create()
                .hasAnnotation(AnnotationQuery.create().annotation(Patch.class))
                .className("");


        PatchLib.scan(query);

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
     *
     * Note: There may be some issues with certain ascii characters if they are not included in the users windows language, but
     * this has to be tested for first.*/
    public void selfAttachAgent() {
        ModSpecAPI modSpec = Global.getSettings().getModManager().getModSpec("patchlib");
        String agentJar = modSpec.getPath() + "/jars/PatchLibAgent.jar";

        String pid = Long.toString(ProcessHandle.current().pid());

        try {
            VirtualMachine vm = VirtualMachine.attach(pid);
            vm.loadAgent(agentJar);
            vm.detach();
        } catch (AgentLoadException | AgentInitializationException | IOException | AttachNotSupportedException ex) {
            throw new RuntimeException("PatchLib failed to attach to the game. \n" +
                    "Check PatchLib's forum thread for guidance on this issue.\n", ex);
        }

    }



}
