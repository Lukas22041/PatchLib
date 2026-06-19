package patch_lib;

import com.fs.starfarer.api.BaseModPlugin;
import com.fs.starfarer.api.Global;
import patch_lib.agent.PatchLibAgentManager;
import patch_lib.install.InstallerLauncher;

public class PatchLibModPlugin extends BaseModPlugin {

    //Attempts to check if the java agent is installed
    static {
        try {
            String agentVersion = System.getProperty("patch_lib.agent.version");
            String modVersion = Global.getSettings().getModManager().getModSpec("patch_lib").getVersion();
            evaluate(agentVersion, modVersion);
        } catch (Throwable t) {

        }
    }

    /** Checks that the agent loaded and matches the mod version. Authoritative second round,
     * in case the early static check could not run yet. */
    public void checkAgentInstall() {
        String agentVersion = System.getProperty("patch_lib.agent.version");
        String modVersion = Global.getSettings().getModManager().getModSpec("patch_lib").getVersion();
        evaluate(agentVersion, modVersion);
    }

    //Opens the installer and quits the game if the agent is missing or a different version.
    private static void evaluate(String agentVersion, String modVersion) {
        //The agent never loaded. Usually a fresh install or a Starsector update that reset the launcher file.
        if (agentVersion == null) {
            InstallerLauncher.launchAndExit(InstallerLauncher.Reason.MISSING_AGENT, modVersion, null);
            return; // launchAndExit calls System.exit on success
        }

        //The agent loaded but is a different version than the mod. Usually a mod update.
        if (!agentVersion.equals(modVersion)) {
            InstallerLauncher.launchAndExit(InstallerLauncher.Reason.VERSION_MISMATCH, modVersion, agentVersion);
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
