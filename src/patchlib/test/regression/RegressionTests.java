package patchlib.test.regression;

import patchlib.agent.PatchLibLogger;
import patchlib.test.targets.RegressionTarget;

import java.util.Objects;

/** One check per patch annotation, each asserting the effect of its handler in RegressionPatches. */
public class RegressionTests {

    public static void run() {
        RegressionTarget target = new RegressionTarget();
        check("@Before return replaced", "patched", target.beforeTarget());
        check("@After return adjusted", 42, target.afterTarget());
        check("@Except exception suppressed", "suppressed", callExcept(target));
        check("@After-only throw propagates", "threw IllegalStateException", callAfterThrow(target));
        check("@After-only throw skips handler", false, RegressionPatches.afterThrowRan);
        check("@RedirectCall arg replaced", 5, target.redirectCallTarget());
        check("@RedirectNew instance swapped", 99, target.redirectNewTarget());
        check("@RedirectFieldRead value replaced", 21, target.redirectReadTarget());
        target.redirectWriteTarget(10);
        check("@RedirectFieldWrite value doubled", 20, target.writeField);
    }

    /** The unpatched method throws, a pass means the @Except handler suppressed it. */
    private static Object callExcept(RegressionTarget target) {
        try {
            return target.exceptTarget();
        } catch (Throwable thrown) {
            return "threw " + thrown.getClass().getSimpleName();
        }
    }

    /** The target throws and only has an @After patch, so the exception must reach the caller. */
    private static Object callAfterThrow(RegressionTarget target) {
        try {
            return target.afterThrowTarget();
        } catch (Throwable thrown) {
            return "threw " + thrown.getClass().getSimpleName();
        }
    }

    private static void check(String name, Object expected, Object actual) {
        if (Objects.equals(expected, actual)) PatchLibLogger.info("TEST " + name + ": PASS");
        else PatchLibLogger.error("TEST " + name + ": FAIL, expected " + expected + ", got " + actual);
    }

}
