package patch_lib;

import com.fs.starfarer.api.BaseModPlugin;
import com.fs.starfarer.api.Global;
import patch_lib.agent.PatchLibAgentManager;

public class PatchLibModPlugin extends BaseModPlugin {

    /** Checks if the agent has loaded and is the same version as the mod */
    public void checkAgentInstall() {
        String agentVersion = System.getProperty("patch_lib.agent.version");
        String patchLibVersion = Global.getSettings().getModManager().getModSpec("patch_lib").getVersion();

        if (agentVersion == null) {
            throw new RuntimeException(
                    "PatchLib could not successfully launch. " +
                    "This either happens because of a wrongful mod installation or because a Starsector update reset the mods required installation. " +
                    "Check PatchLib's forum thread for installation instructions.");

            //System.exit(0);
        }

        if (!agentVersion.equals(patchLibVersion)) {

            String fileLocation = System.getProperty("user.dir");

            throw new RuntimeException(
                    "PatchLib could not start due to a mismatch in the mods and agents version.\n\n" +
                            "" +
                            "Mod Version: " + patchLibVersion + "\n" +
                            "Agent Version: " + agentVersion + "\n\n" +
                            "" +
                            "In most cases, this can be fixed by copying the \"PatchLibAgent.jar\" file from the PatchLib mod folder in to \"" + fileLocation + "\"" +
                            ".") ;

            //System.exit(0);
        }

    }

    @Override
    public void onApplicationLoad() throws Exception {
        checkAgentInstall();

        //Starts the agents mod scan & patches.
        //Should be called before most other mods have their onApplicationLoad called, due to the ! at the start of the mods name, bringing it earlier in to load.
        //Purposefully called from the mods code, as it prevents the agent from running any patches if the mod is disabled.
        PatchLibAgentManager.getInstance().init(this.getClass().getClassLoader());
    }

    @Override
    public void onGameLoad(boolean newGame) {

    }

    @Override
    public void onNewGame() {

    }

    @Override
    public void onNewGameAfterEconomyLoad() {

    }

    @Override
    public void onNewGameAfterProcGen() {

    }

    @Override
    public void onNewGameAfterTimePass() {

    }


}
