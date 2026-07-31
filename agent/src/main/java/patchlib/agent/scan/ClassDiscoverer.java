package patchlib.agent.scan;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.ModSpecAPI;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.ClassFileLocator;
import net.bytebuddy.pool.TypePool;
import patchlib.agent.data.ClassDataImpl;
import patchlib.agent.log.PatchLibLogger;
import patchlib.api.data.ClassData;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public class ClassDiscoverer {

    public record JarSource(ModSpecAPI mod, File jar) { }

    private final List<String> gameJars = List.of("starfarer.api.jar", "starfarer_obf.jar", "fs.common_obf.jar", "fs.sound_obf.jar");

    public DiscoveryData discover() {
        PatchLibLogger.blank();
        PatchLibLogger.info("Starting class discovery");

        long before = System.currentTimeMillis();
        DiscoveryData data = discoverClasses();
        long delta = System.currentTimeMillis() - before;

        PatchLibLogger.info("Discovered " + data.classes().size() + " classes in " + delta + "ms");

        PatchLibLogger.info("Finished class discovery");
        PatchLibLogger.blank();
        return data;
    }

    private DiscoveryData discoverClasses() {
        List<ClassData> classDataList = new ArrayList<>();

        List<JarSource> jars = getAllJars();

        List<ClassFileLocator> locators = getLocators(jars);

        ExecutorService executorService = createExecutor();

        try {

            ClassFileLocator locator = new ClassFileLocator.Compound(locators);

            //Create a type pool that doesn't read method bodies
            TypePool.CacheProvider cache = new TypePool.CacheProvider.Simple();
            TypePool pool = new TypePool.Default(cache, locator, TypePool.Default.ReaderMode.FAST);

            List<ClassDiscoverTask> tasks = new ArrayList<>();
            for (JarSource jarSource : jars) {
                boolean isStarsectorClass = gameJars.contains(jarSource.jar.getName());
                ClassDiscoverTask task = new ClassDiscoverTask(jarSource, pool, isStarsectorClass);
                tasks.add(task);
            }

            List<Future<ClassDiscoverTask.ClassDiscoverTaskResult>> futures = executorService.invokeAll(tasks);

            for (int i = 0; i < futures.size(); i++) {
                JarSource jar = jars.get(i);

                try {
                    ClassDiscoverTask.ClassDiscoverTaskResult result = futures.get(i).get();
                    classDataList.addAll(result.classDataList());
                } catch (Exception ex) {
                    PatchLibLogger.error("Failed to parse jar " + jar.jar.getPath(), ex);
                }
            }

            return new DiscoveryData(pool, locator, classDataList);

        }  catch (Exception ex) {
            PatchLibLogger.error("Ran in to an error while scanning game & mod jars.");
            throw new RuntimeException(ex);
        } finally {
            executorService.shutdown();
        }

    }

    private ExecutorService createExecutor() {
        int threads = Math.max(1, Math.min(4, Runtime.getRuntime().availableProcessors()-1));

        AtomicInteger count = new AtomicInteger();

        return Executors.newFixedThreadPool(threads, runnable -> {
            Thread thread = new Thread(runnable, "PatchLib-" + count.incrementAndGet());
            thread.setDaemon(true);
            return thread;
        });
    }

    private List<ClassFileLocator> getLocators(List<JarSource> jars) {
        List<ClassFileLocator> locators = new ArrayList<>();
        locators.add(ClassFileLocator.ForClassLoader.ofSystemLoader()); //Required to read JVM and game Classes that appear on the annotations
        locators.add(ClassFileLocator.ForClassLoader.of(ClassDiscoverer.class.getClassLoader()));

        for (JarSource jar : jars) {
            try {
                locators.add(ClassFileLocator.ForJarFile.of(jar.jar));
            } catch (IOException ex) {
                PatchLibLogger.error("Could not add " + jar.jar.getName() + " to classfile locators", ex);
            }
        }

        return locators;
    }

    private List<JarSource> getAllJars() {
        List<ModSpecAPI> mods = Global.getSettings().getModManager().getEnabledModsCopy();

        List<JarSource> jars = new ArrayList<>();

        //Add the games own jars
        /*String workingDir = System.getProperty("user.dir");
        for (String gameJar : gameJars) {
            File jar = new File(workingDir, gameJar);
            jars.add(new JarSource(null, jar));
            PatchLibLogger.info("Discovered jar: " + jar.getName());
        }*/

        File coreDir = new File(System.getProperty("user.dir"));
        for (File coreJar : coreDir.listFiles((dir, name) -> name.endsWith(".jar"))) {
            jars.add(new JarSource(null, coreJar));
            PatchLibLogger.info("Discovered jar: " + coreJar.getName());
        }

        //Add mod jars
        for (ModSpecAPI mod : mods) {
            for (String jarPath : mod.getJars()) {
                File jar = new File(mod.getPath(), jarPath);
                if (!jar.exists()) {
                    PatchLibLogger.error("Could not find jar " + jar.getPath() + ", the mod might have an invalid mod_info.json entry.");
                }
                jars.add(new JarSource(mod, jar));
                PatchLibLogger.info("Discovered jar: " + jar.getName() + " from " + mod.getName());
            }
        }

        return jars;
    }

}
