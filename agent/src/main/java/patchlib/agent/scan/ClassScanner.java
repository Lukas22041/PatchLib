package patchlib.agent.scan;

import patchlib.agent.data.ClassDataImpl;
import patchlib.api.data.ClassData;
import patchlib.api.query.ClassQuery;
import patchlib.api.spec.ClassQuerySpec;

import java.util.List;

public class ClassScanner {

    private DiscoveryData data;

    public ClassScanner(DiscoveryData data) {
        this.data = data;
    }

    public List<ClassData> scan(ClassQuerySpec querySpec, boolean excludeGameClasses, boolean excludeModClasses) {

        List<ClassData> classes = data.classes();

        if (excludeGameClasses) {
            classes = classes.stream().filter(classData -> classData.getSourceMod() != null).toList();
        }

        if (excludeModClasses) {
            classes = classes.stream().filter(classData -> classData.getSourceMod() == null).toList();
        }

        //Turn builder in to match specs here, then create matchers out of them.

        return null;
    }

}
