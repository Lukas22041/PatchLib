package patchlib.test.tests;

import patchlib.test.TestResult;
import patchlib.test.targets.BeforeTestTarget;

import java.util.ArrayList;
import java.util.List;

public class BeforeTests {

    public static List<TestResult> runTests() {
        List<TestResult> results = new ArrayList<>();
        results.add(testReplaceArg());
        results.add(testSkipMethodTarget());
        return results;
    }

    //Test the patch replacing the argument.
    public static TestResult testReplaceArg() {
        BeforeTestTarget target = new BeforeTestTarget();

        String input = "Test";
        String result = target.testReplaceArgTarget(input);
        boolean failed = !result.equals(input + "_REPLACED");

        return new TestResult("testReplaceArg", failed, "The argument was not replaced");
    }

    //Test skipping the method content, the original method modifies the output, which should be skipped.
    public static TestResult testSkipMethodTarget() {
        BeforeTestTarget target = new BeforeTestTarget();

        String input = "Test";
        String result = target.testSkipMethodTarget(input);
        boolean failed = !result.equals(input);

        return new TestResult("testSkipMethodTarget", failed, "The method content was not skipped");
    }

}
