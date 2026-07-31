package patchlib.test.tests;

import patchlib.test.TestResult;
import patchlib.test.targets.AfterTestTarget;
import patchlib.test.targets.RedirectTestTarget;

import java.util.ArrayList;
import java.util.List;

public class RedirectTests {

    public static List<TestResult> runTests() {
        List<TestResult> results = new ArrayList<>();
        results.add(testMethodCallRedirect());
        results.add(testConstructorCallRedirect());
        results.add(testFieldReadRedirect());
        results.add(testFieldWriteRedirect());
        results.add(testRedirectLayers());
        return results;
    }

    public static TestResult testMethodCallRedirect() {
        RedirectTestTarget target = new RedirectTestTarget();

        String result = target.testMethodCallRedirect();
        boolean failed = !result.equals("TEST" + "_REPLACED");

        return new TestResult("testMethodCallRedirect", failed, "The call was not redirected");
    }

    public static TestResult testConstructorCallRedirect() {
        RedirectTestTarget target = new RedirectTestTarget();

        String result = target.testConstructorCallRedirect();
        boolean failed = !result.equals("TEST" + "_REPLACED");

        return new TestResult("testConstructorCallRedirect", failed, "The call was not redirected");
    }

    public static TestResult testFieldReadRedirect() {
        RedirectTestTarget target = new RedirectTestTarget();

        String result = target.testFieldReadRedirect();
        boolean failed = !result.equals("TEST" + "_REPLACED");

        return new TestResult("testFieldReadRedirect", failed, "The read was not redirected");
    }

    public static TestResult testFieldWriteRedirect() {
        RedirectTestTarget target = new RedirectTestTarget();

        String result = target.testFieldWriteRedirect();
        boolean failed = !result.equals("TEST" + "_REPLACED");

        return new TestResult("testFieldWriteRedirect", failed, "The field write was not redirected");
    }


    public static TestResult testRedirectLayers() {
        RedirectTestTarget target = new RedirectTestTarget();

        String result = target.testRedirectLayers();
        boolean failed = !result.equals("123");

        return new TestResult("testRedirectLayers", failed, "The layers did not run in the right order");
    }

}
