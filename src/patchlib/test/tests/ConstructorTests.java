package patchlib.test.tests;

import patchlib.test.TestResult;
import patchlib.test.targets.ConstructorTestTarget;

import java.util.ArrayList;
import java.util.List;

public class ConstructorTests {

    public static List<TestResult> runTests() {
        List<TestResult> results = new ArrayList<>();
        results.add(testConstructor());
        return results;
    }

    //The before patch replaces the argument, the after patch modifies the constructed instance.
    public static TestResult testConstructor() {
        String input = "Test";
        ConstructorTestTarget target = new ConstructorTestTarget(input);
        boolean failed = !target.value.equals(input + "_BEFORE_AFTER");

        return new TestResult("testConstructor", failed, "The constructor patches did not both apply");
    }

}
