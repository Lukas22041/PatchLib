package patchlib.agent.scan;

import patchlib.api.data.ClassData;
import patchlib.api.query.ClassQuery;

import java.util.List;

public class ClassScanner {

    private List<ClassData> classes;

    public ClassScanner(List<ClassData> classes) {
        this.classes = classes;
    }

    public static List<ClassData> scan(ClassQuery builder) {

        //Turn builder in to match specs here, then create matchers out of them.

        return null;
    }

}
