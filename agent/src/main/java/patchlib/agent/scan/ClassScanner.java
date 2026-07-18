package patchlib.agent.scan;

import patchlib.api.data.ClassData;
import patchlib.api.scan.ClassScanBuilder;

import java.util.List;

public class ClassScanner {

    private List<ClassData> classes;

    public ClassScanner(List<ClassData> classes) {
        this.classes = classes;
    }

    public static List<ClassData> scan(ClassScanBuilder builder) {

        //Turn builder in to match specs here, then create matchers out of them.

        return null;
    }

}
