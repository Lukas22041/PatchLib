package patchlib.agent.scan;

import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.matcher.ElementMatcher;
import patchlib.agent.log.PatchLibLogger;
import patchlib.agent.matchers.ClassMatcher;
import patchlib.api.data.ClassData;
import patchlib.api.spec.ClassQuerySpec;

import java.util.ArrayList;
import java.util.List;

public class ClassScanner {

    private DiscoveryData data;
    private final ClassLoader modClassLoader;

    public ClassScanner(DiscoveryData data, ClassLoader modClassLoader) {
        this.data = data;
        this.modClassLoader = modClassLoader;
    }

    public synchronized List<ClassData> scan(ClassQuerySpec querySpec, boolean excludeGameClasses, boolean excludeModClasses) {

        long start = System.currentTimeMillis();

        ArrayList<ClassData> classes = new ArrayList<>();
        for (DiscoveredClass discoveredClass : match(querySpec, excludeGameClasses, excludeModClasses)) {
            String name = discoveredClass.type().getName();
            ClassLoader loader = discoveredClass.sourceMod() != null ? modClassLoader : ClassScanner.class.getClassLoader();

            try {
                //Load with "initialize" set to false prevents static blocks from being called early.
                Class<?> type = Class.forName(name, false, loader);
                classes.add(new ClassData(type, discoveredClass.sourceMod(), discoveredClass.isFromStarsector()));
            } catch (Exception | LinkageError ex) {
                PatchLibLogger.warn("Skipped " + name + " in a scan, as it could not be loaded: " + ex);
            }
        }

        long diff = System.currentTimeMillis() - start;

        PatchLibLogger.info("Returned " + classes.size() + " classes in " + diff + "ms from query: " + querySpec);

        return classes;
    }

    /** Matches without loading the classes, for internal use before the patches are installed. */
    public synchronized List<DiscoveredClass> match(ClassQuerySpec querySpec, boolean excludeGameClasses, boolean excludeModClasses) {
        if (data == null) {
            throw new IllegalStateException("PatchLib.scan() can only be used during onApplicationLoad, its data is released once a save is loaded.");
        }

        ElementMatcher.Junction<TypeDescription> matcher = ClassMatcher.fromQuery(querySpec);

        List<DiscoveredClass> matches = new ArrayList<>();
        for (DiscoveredClass discoveredClass : data.classes()) {
            if (excludeGameClasses && discoveredClass.sourceMod() == null) continue;
            if (excludeModClasses && discoveredClass.sourceMod() != null) continue;

            if (matcher.matches(discoveredClass.type())) {
                matches.add(discoveredClass);
            }
        }
        return matches;
    }

    /** Releases the discovered classes and closes their jars. Scanning is no longer possible after this. */
    public synchronized void close() {
        if (data == null) return;
        data.close();
        data = null;
        PatchLibLogger.info("Released the class scan data");
    }

}
