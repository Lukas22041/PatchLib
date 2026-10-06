package patchlib.test.targets;

public class ExceptTestTarget {

    public String testSuppressExceptionTarget(String input) {
        throw new RuntimeException("TEST");
    }

    public void testReplaceExceptionTarget() {
        throw new RuntimeException("TEST");
    }

    public int testSuppressPrimitiveTarget(int input) {
        throw new RuntimeException("TEST");
    }

    public String testExceptAndAfterTarget(String input) {
        throw new RuntimeException("TEST");
    }

}
