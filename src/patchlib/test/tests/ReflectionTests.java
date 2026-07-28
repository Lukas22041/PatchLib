package patchlib.test.tests;

import patchlib.test.TestResult;
import patchlib.test.targets.AfterTestTarget;
import patchlib.test.targets.ReflectionTestTarget;

import java.util.ArrayList;
import java.util.List;

public class ReflectionTests {

    public static List<TestResult> runTests() {
        List<TestResult> results = new ArrayList<>();
        results.add(testReflection());
        return results;
    }

    public static TestResult testReflection() {
        ReflectionTestTarget target = new ReflectionTestTarget();

        String input = "Test";
        String result = target.testTarget();
        boolean failed = !result.equals("Test1Test2");

        if (result.contains("EXCEPTION")) {
            return new TestResult("testReflection", true, "The error threw with: " + result);
        }

        return new TestResult("testReflection", failed, "The reflection call failed");
    }

}
