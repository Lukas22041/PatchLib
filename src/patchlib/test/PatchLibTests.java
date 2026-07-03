package patchlib.test;

import patchlib.agent.PatchLibLogger;
import patchlib.test.performance.PerformanceTests;
import patchlib.test.regression.RegressionTests;

/** In-game test suite. Runs from the mod plugin after the agent is initialized and game classes are preloaded,
 * so test targets get transformed when the tests first load them. */
public class PatchLibTests {

    /** Master switch for the in-game test suite. Off for releases. */
    public static final boolean ENABLED = true;

    public static void runIfEnabled() {
        if (!ENABLED) return;
        PatchLibLogger.info("Running in-game tests");
        RegressionTests.run();
        PerformanceTests.run();
        PatchLibLogger.info("In-game tests finished");
    }

}
