package patchlib.test.tests;

import patchlib.test.TestResult;
import patchlib.test.targets.AfterTestTarget;
import patchlib.test.targets.ExceptTestTarget;

import java.util.ArrayList;
import java.util.List;

public class ExceptTests {

    public static List<TestResult> runTests() {
        List<TestResult> results = new ArrayList<>();
        results.add(testSuppressException());
        results.add(testReplaceException());
        results.add(testSuppressPrimitive());
        results.add(testExceptAndAfter());
        return results;
    }

    //Test the patch replacing the argument.
    public static TestResult testSuppressException() {
        ExceptTestTarget target = new ExceptTestTarget();
        try {
            String input = "Test";
            String result = target.testSuppressExceptionTarget(input);
            boolean failed = !result.equals(input + "_SUPPRESSED");
            return new TestResult("testSuppressException", failed, "The exception was suppressed, but returned the wrong value");
        } catch (Exception ex) {
            return new TestResult("testSuppressException", true, "The exception was not suppressed");
        }
    }

    public static TestResult testReplaceException() {
        ExceptTestTarget target = new ExceptTestTarget();
        try {
            target.testReplaceExceptionTarget();
            return new TestResult("testReplaceException", true, "No exception was thrown");
        } catch (Exception ex) {
            boolean failed = !ex.getMessage().equals("TEST_REPLACED");
            return new TestResult("testReplaceException", failed, "The exception was not replaced");
        }
    }

    public static TestResult testSuppressPrimitive() {
        ExceptTestTarget target = new ExceptTestTarget();
        try {
            int result = target.testSuppressPrimitiveTarget(5);
            return new TestResult("testSuppressPrimitive", result != 5, "The exception was suppressed, but returned the wrong value");
        } catch (Exception ex) {
            return new TestResult("testSuppressPrimitive", true, "The exception was not suppressed");
        }
    }

    //The after patch should run on the suppressed return value.
    public static TestResult testExceptAndAfter() {
        ExceptTestTarget target = new ExceptTestTarget();
        try {
            String input = "Test";
            String result = target.testExceptAndAfterTarget(input);
            boolean failed = !result.equals(input + "_SUPPRESSED_AFTER");
            return new TestResult("testExceptAndAfter", failed, "The after patch did not run on the suppressed value");
        } catch (Exception ex) {
            return new TestResult("testExceptAndAfter", true, "The exception was not suppressed");
        }
    }

}
