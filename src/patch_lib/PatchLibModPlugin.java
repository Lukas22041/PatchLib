package patch_lib;

import com.fs.starfarer.api.BaseModPlugin;

public class PatchLibModPlugin extends BaseModPlugin {

    /*This method is run right at the end of starsectors loading.
    * It is most useful for loading data that only really needs to be setup once. */
    @Override
    public void onApplicationLoad() throws Exception {
        throw new RuntimeException("Template mod loaded!" +
                "\nThis crash is used to test if your mod setup works correctly (it does)." +
                "\nFix it by removing the \"RuntimeException\" lines in your mod plugin.");
    }

    /*This method is run in two cases:
    * - At the end of the creation of a new save
    * - When an existing save finished loading
    * This method is most useful for adding transient listeners/scripts and for enabling mid-save compatibility,
    * like adding star systems to an existing save if the mod was just added. */
    @Override
    public void onGameLoad(boolean newGame) {

    }

    /*Runs when a save is created.
    * This method specifically runs before procedural generation, so any base-game procedural content is not accessible yet.
    * It is recommended to start placing your modded star systems from here,
    * as starsectors procgen will avoid placing stars and hyperspace storms nearby existing systems, preventing overlap.*/
    @Override
    public void onNewGame() {

    }

    /*Runs after onNewGame, after the economy has finished loading.
    * This method can be useful for accessing other mods star systems, assuming those have placed their systems in onNewGame. */
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
