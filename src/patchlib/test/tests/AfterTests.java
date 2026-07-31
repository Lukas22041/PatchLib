package patchlib.test.tests;

import patchlib.test.TestResult;
import patchlib.test.targets.AfterTestTarget;
import patchlib.test.targets.BeforeTestTarget;

import java.util.ArrayList;
import java.util.List;

public class AfterTests {

    public static List<TestResult> runTests() {
        List<TestResult> results = new ArrayList<>();
        results.add(testReplaceReturnValue());
        return results;
    }

    //Test the patch replacing the argument.
    public static TestResult testReplaceReturnValue() {
        AfterTestTarget target = new AfterTestTarget();

        String input = "Test";
        String result = target.testReplaceReturnValueTarget(input);
        boolean failed = !result.equals(input + "_REPLACED");

        return new TestResult("testReplaceReturnValue", failed, "The return value was not replaced");
    }

}
