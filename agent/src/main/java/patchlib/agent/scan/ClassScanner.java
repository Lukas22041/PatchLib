package patchlib.agent.scan;

import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.matcher.ElementMatcher;
import patchlib.agent.data.ClassDataImpl;
import patchlib.agent.log.PatchlibLogger;
import patchlib.agent.matchers.ClassMatcher;
import patchlib.api.PatchLibImpl;
import patchlib.api.data.ClassData;
import patchlib.api.query.ClassQuery;
import patchlib.api.spec.ClassQuerySpec;

import java.util.ArrayList;
import java.util.List;

public class ClassScanner {

    private DiscoveryData data;

    public ClassScanner(DiscoveryData data) {
        this.data = data;
    }

    public List<ClassData> scan(ClassQuerySpec querySpec, boolean excludeGameClasses, boolean excludeModClasses) {

        long start = System.currentTimeMillis();

        List<ClassData> classes = data.classes();

        if (excludeGameClasses) {
            classes = classes.stream().filter(classData -> classData.getSourceMod() != null).toList();
        }

        if (excludeModClasses) {
            classes = classes.stream().filter(classData -> classData.getSourceMod() == null).toList();
        }

        ElementMatcher.Junction<TypeDescription> matcher = ClassMatcher.fromQuery(querySpec);

        ArrayList<ClassData> filtered = new ArrayList<>();
        for (ClassData classData : classes) {
            ClassDataImpl classDataImpl = (ClassDataImpl) classData;
            TypeDescription description = classDataImpl.getTypeDescription();
            if (matcher.matches(description)) {
                filtered.add(classData);
            }
        }

        long diff = System.currentTimeMillis() - start;

        PatchlibLogger.info("Returned " + filtered.size() + " classes in " + diff + "ms from query: " + querySpec);

        return filtered;
    }

}
