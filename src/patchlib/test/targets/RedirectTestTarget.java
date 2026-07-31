package patchlib.test.targets;

public class RedirectTestTarget {

    private String testField = "TEST";
    private String testCall(String input) {
        return input;
    };
    public static class RedirectConstructorTest {
        public String testField;
        public RedirectConstructorTest(String testField) {
            this.testField = testField;
        }
    }




    public String testMethodCallRedirect() {
        String input = "TEST";
        return testCall(input);
    }

    public String testConstructorCallRedirect() {
        RedirectConstructorTest testObject = new RedirectConstructorTest("TEST");
        return testCall(testObject.testField);
    }

    public String testFieldReadRedirect() {
        return testField;
    }

    public String testFieldWriteRedirect() {
        testField = "TEST";
        return testField;
    }

    public String testRedirectLayers() {
        String input = "1";
        return testCall(input);
    }

}
