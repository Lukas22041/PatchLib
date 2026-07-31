package patchlib.test.targets;

public class ReflectionTestTarget {

    private String testField = "Test1";
    private String testMethod() {
        return "Test2";
    }

    public String testTarget() {
        return "";
    }

}
