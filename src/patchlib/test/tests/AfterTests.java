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
        results.add(testVoid());
        results.add(testPrimitiveReturn());
        results.add(testBeforeAndAfter());
        results.add(testStatic());
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

    public static TestResult testVoid() {
        AfterTestTarget target = new AfterTestTarget();
        target.testVoidTarget();
        return new TestResult("testVoid", !target.afterVoidRan, "The patch did not run on a void method");
    }

    public static TestResult testPrimitiveReturn() {
        AfterTestTarget target = new AfterTestTarget();
        int result = target.testPrimitiveReturnTarget(5);
        return new TestResult("testPrimitiveReturn", result != 10, "The primitive return value was not replaced");
    }

    //The after patch should receive the result of the argument replaced by the before patch.
    public static TestResult testBeforeAndAfter() {
        AfterTestTarget target = new AfterTestTarget();

        String input = "Test";
        String result = target.testBeforeAndAfterTarget(input);
        boolean failed = !result.equals(input + "_BEFORE_AFTER");

        return new TestResult("testBeforeAndAfter", failed, "The before and after patches did not both apply");
    }

    public static TestResult testStatic() {
        String input = "Test";
        String result = AfterTestTarget.testStaticTarget(input);
        boolean failed = !result.equals(input + "_REPLACED");

        return new TestResult("testStatic", failed, "The return value of a static method was not replaced");
    }

}
