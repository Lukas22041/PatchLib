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
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public class ClassDiscoverer {

    record JarSource(ModSpecAPI mod, File jar) { }

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

        try {

            ClassFileLocator locator = new ClassFileLocator.Compound(locators);

            //Create a type pool that doesn't read method bodies
            TypePool.CacheProvider cache = new TypePool.CacheProvider.Simple();
            TypePool pool = new TypePool.Default(cache, locator, TypePool.Default.ReaderMode.FAST);

            for (JarSource jarSource : jars) {
                try (JarFile jarFile = new JarFile(jarSource.jar) ){
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
                            ClassData classData = new ClassDataImpl(typeDescription, jarSource.mod);
                            classDataList.add(classData);
                            count++;
                        } catch (Exception ex) {
                            PatchLibLogger.error("Could not resolve type " + binaryName);
                        }

                    }
                    PatchLibLogger.info("Discovered " + count + " classes in " + jarFile.getName());

                }
            }

            return new DiscoveryData(pool, locator, classDataList);

        } catch (IOException ex) {
            PatchLibLogger.error("Ran in to an IOException during class loading.");
            throw new RuntimeException(ex);
        }

    }

    private List<ClassFileLocator> getLocators(List<JarSource> jars) {
        List<ClassFileLocator> locators = new ArrayList<>();
        locators.add(ClassFileLocator.ForClassLoader.ofSystemLoader()); //Required to read JVM and game Classes that appear on the annotations
        locators.add(ClassFileLocator.ForClassLoader.of(ClassDiscoverer.class.getClassLoader()));

        for (JarSource jar : jars) {
            try {
                locators.add(ClassFileLocator.ForJarFile.of(jar.jar));
            } catch (IOException ex) {
                PatchLibLogger.error("Could not add " + jar.jar.getName() + " to classfile locators");
            }
        }

        return locators;
    }

    private List<JarSource> getAllJars() {
        List<ModSpecAPI> mods = Global.getSettings().getModManager().getEnabledModsCopy();

        List<JarSource> jars = new ArrayList<>();

        //Add the games own jars
        String workingDir = System.getProperty("user.dir");
        for (String gameJar : gameJars) {
            File jar = new File(workingDir, gameJar);
            jars.add(new JarSource(null, jar));
            PatchLibLogger.info("Discovered jar: " + jar.getName());
        }

        //Add mod jars
        for (ModSpecAPI mod : mods) {
            for (String jarPath : mod.getJars()) {
                File jar = new File(mod.getPath(), jarPath);
                jars.add(new JarSource(mod, jar));
                PatchLibLogger.info("Discovered jar: " + jar.getName() + " from " + mod.getName());
            }
        }

        return jars;
    }

}
