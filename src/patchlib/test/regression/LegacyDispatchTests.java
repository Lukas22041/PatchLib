package patchlib.test.regression;

import org.codehaus.janino.SimpleCompiler;
import patchlib.agent.PatchLibLogger;

import java.util.Objects;

/** Legacy dispatch checks on a janino compiled target. Real script classes are class file 45,
 * below what constant dispatch supports, so the id-based path stays in use and needs its own
 * coverage. The target is compiled here with the game's janino, which emits the same class file
 * version as real scripts, and gets patched by LegacyDispatchPatches on load. */
public class LegacyDispatchTests {

    public static void run() {
        LegacyTarget target = compileTarget();
        if (target == null) return;
        check("@Before on legacy dispatch", "patched", target.beforeTarget());
        check("@After on legacy dispatch", 42, target.afterTarget());
    }

    private static LegacyTarget compileTarget() {
        try {
            SimpleCompiler compiler = new SimpleCompiler();
            //The parent loader makes the LegacyTarget interface visible to the compiled class.
            compiler.setParentClassLoader(LegacyDispatchTests.class.getClassLoader());
            compiler.cook("public class PatchLibLegacyTarget implements patchlib.test.regression.LegacyTarget {"
                    + " public String beforeTarget() { return \"original\"; }"
                    + " public int afterTarget() { return 1; } }");
            return (LegacyTarget) compiler.getClassLoader().loadClass("PatchLibLegacyTarget").newInstance();
        } catch (Throwable t) {
            PatchLibLogger.error("TEST legacy dispatch: FAIL, could not compile the target: " + t);
            return null;
        }
    }

    private static void check(String name, Object expected, Object actual) {
        if (Objects.equals(expected, actual)) PatchLibLogger.info("TEST " + name + ": PASS");
        else PatchLibLogger.error("TEST " + name + ": FAIL, expected " + expected + ", got " + actual);
    }

}
