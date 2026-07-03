package patchlib.test.targets;

/** Patched by RegressionPatches. Each method is the target of exactly one regression test. */
public class RegressionTarget {

    public int readField = 7;
    public int writeField = 0;

    public String beforeTarget() {
        return "original";
    }

    public int afterTarget() {
        return 1;
    }

    public String exceptTarget() {
        throw new IllegalStateException("boom");
    }

    public int redirectCallTarget() {
        return callee(1);
    }

    public int callee(int x) {
        return x;
    }

    public int redirectNewTarget() {
        return new TestBox(1).value;
    }

    public int redirectReadTarget() {
        return readField;
    }

    public void redirectWriteTarget(int v) {
        writeField = v;
    }

}
