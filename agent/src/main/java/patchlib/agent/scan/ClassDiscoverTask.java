package patchlib.agent.scan;

import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.pool.TypePool;
import patchlib.agent.log.PatchLibLogger;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/** One task per jar that scans for class files in parallel on multiple threads. */
public class ClassDiscoverTask implements Callable<ClassDiscoverTask.ClassDiscoverTaskResult> {

    public record ClassDiscoverTaskResult(String path, List<DiscoveredClass> classes, List<String> unresolvedClasses) { }

    private final ClassDiscoverer.JarSource jarSource;
    private final TypePool pool;
    private final boolean isStarsectorJar;

    public ClassDiscoverTask(ClassDiscoverer.JarSource jarSource, TypePool pool, boolean isStarsectorJar) {
        this.jarSource = jarSource;
        this.pool = pool;
        this.isStarsectorJar = isStarsectorJar;
    }

    @Override
    public ClassDiscoverTaskResult call() throws Exception {

        List<DiscoveredClass> classes = new ArrayList<>();

        try (JarFile jarFile = new JarFile(jarSource.jar()) ){
            Enumeration<JarEntry> entries = jarFile.entries();

            int count = 0;
            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                String name = entry.getName();
                if (entry.isDirectory()) continue;
                if (!name.endsWith(".class")) continue;
                if (name.contains("module-info.class")) continue;

                //Create actual full class path in package format.
                String binaryName = name.substring(0, name.length() - ".class".length())
                        .replace('/', '.');

                try {
                    TypeDescription typeDescription = pool.describe(binaryName).resolve();
                    classes.add(new DiscoveredClass(typeDescription, jarSource.mod(), isStarsectorJar));
                    count++;
                } catch (Exception ex) {
                    PatchLibLogger.error("Could not resolve type " + binaryName);
                }

            }
            PatchLibLogger.info("Discovered " + count + " classes in " + jarFile.getName());
        }

        return new ClassDiscoverTaskResult(jarSource.jar().getPath(), classes, null);
    }
}
