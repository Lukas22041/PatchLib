package patchlib.test;

import com.fs.starfarer.api.Global;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import patchlib.test.tests.*;

import java.util.ArrayList;
import java.util.List;

/** Runs a bunch of regression tests at game runtime, to ensure that a recent change didn't break functionality and to check for potentially
 * issues on certain JVMs. */
public class PatchLibTests {

    private static final String PREFIX = "[PatchLib] ";
    private static Logger log = Global.getLogger(PatchLibTests.class);
    static {
        log.setLevel(Level.ALL);
    }

    public void runTests() {
        log.info(PREFIX + "Running tests");

        List<TestResult> results = new ArrayList<>();
        results.addAll(BeforeTests.runTests());
        results.addAll(AfterTests.runTests());
        results.addAll(ExceptTests.runTests());
        results.addAll(RedirectTests.runTests());
        results.addAll(ReflectionTests.runTests());

        List<TestResult> failed = results.stream().filter(test -> test.failed()).toList();
        log.info(PREFIX + (results.size()-failed.size()) + "/" + results.size() + " tests have run successfully");

        for (TestResult fail : failed) {
            log.error("The test \"" + fail.testName() + " failed with the message \"" + fail.failureMessage() + "\"");
        }

        if (!failed.isEmpty()) {
            throw new RuntimeException("PatchLib ran in to an issue during patch testing.");
        }

        log.info(PREFIX + "Finished running tests");
    }



}
