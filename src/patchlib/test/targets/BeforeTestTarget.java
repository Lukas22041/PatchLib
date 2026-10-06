package patchlib.test.targets;

public class BeforeTestTarget {

    public boolean skipVoidRan = false;

    public String testReplaceArgTarget(String input) {
        return input;
    }

    public String testSkipMethodTarget(String input) {
        return input + " added message";
    }

    public void testSkipVoidTarget() {
        skipVoidRan = true;
    }

    public int testSkipPrimitiveTarget(int input) {
        return input + 1;
    }

}
