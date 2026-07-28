package patchlib.test.targets;

public class BeforeTestTarget {

    public String testReplaceArgTarget(String input) {
        return input;
    }

    public String testSkipMethodTarget(String input) {
        return input + " added message";
    }

}
