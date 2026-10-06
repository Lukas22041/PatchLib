package patchlib.test.targets;

public class AfterTestTarget {

    public boolean afterVoidRan = false;

    public String testReplaceReturnValueTarget(String input) {
        return input;
    }

    public void testVoidTarget() {

    }

    public int testPrimitiveReturnTarget(int input) {
        return input;
    }

    public String testBeforeAndAfterTarget(String input) {
        return input;
    }

    public static String testStaticTarget(String input) {
        return input;
    }

}
