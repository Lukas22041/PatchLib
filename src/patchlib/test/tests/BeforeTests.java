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
        results.add(testSkipVoid());
        results.add(testSkipPrimitive());
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

    public static TestResult testSkipVoid() {
        BeforeTestTarget target = new BeforeTestTarget();
        target.testSkipVoidTarget();
        return new TestResult("testSkipVoid", target.skipVoidRan, "The void method content was not skipped");
    }

    public static TestResult testSkipPrimitive() {
        BeforeTestTarget target = new BeforeTestTarget();
        int result = target.testSkipPrimitiveTarget(5);
        return new TestResult("testSkipPrimitive", result != 5, "The primitive method was not skipped with the given return value");
    }

}
