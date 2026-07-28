package patchlib.test.targets;

public class ExceptTestTarget {

    public String testSuppressExceptionTarget(String input) {
        throw new RuntimeException("TEST");
    }

    public void testReplaceExceptionTarget() {
        throw new RuntimeException("TEST");
    }

}
