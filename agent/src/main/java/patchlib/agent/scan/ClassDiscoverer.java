package patchlib.agent.scan;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.ModSpecAPI;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.ClassFileLocator;
import net.bytebuddy.pool.TypePool;
import patchlib.agent.data.ClassDataImpl;
import patchlib.agent.log.PatchlibLogger;
import patchlib.api.data.ClassData;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public class ClassDiscoverer {

    record JarSource(ModSpecAPI mod, File jar) { }

    private List<String> gameJars = List.of("starfarer.api.jar", "starfarer_obf.jar", "fs.common_obf.jar", "fs.sound_obf.jar");

    public List<ClassData> discover() {
        PatchlibLogger.info("Starting class discovery");

        long before = System.currentTimeMillis();
        List<ClassData> classDataList = discoverClasses();
        long delta = System.currentTimeMillis() - before;

        PatchlibLogger.info("Discovered " + classDataList.size() + " classes in " + delta + "ms");

        PatchlibLogger.info("Finished class discovery");
        return classDataList;
    }

    private List<ClassData> discoverClasses() {
        List<ClassData> classDataList = new ArrayList<>();

        List<JarSource> jars = getAllJars();

        List<ClassFileLocator> locators = getLocators(jars);

        try (ClassFileLocator locator = new ClassFileLocator.Compound(locators) ) {

            //Create a type pool that doesn't read method bodies
            TypePool.CacheProvider cache = new TypePool.CacheProvider.Simple();
            TypePool pool = new TypePool.Default(cache, locator, TypePool.Default.ReaderMode.FAST);

            for (JarSource jarSource : jars) {
                try (JarFile jarFile = new JarFile(jarSource.jar) ){
                    Enumeration<JarEntry> entries = jarFile.entries();

                    int count = 0;
                    while (entries.hasMoreElements()) {
                        JarEntry entry = entries.nextElement();
                        if (entry.isDirectory() || !entry.getName().endsWith(".class")) continue;

                        String name = entry.getName();

                        //Create actual full class path in package format.
                        String binaryName = name.substring(0, name.length() - ".class".length())
                                .replace('/', '.');

                        TypeDescription typeDescription = pool.describe(binaryName).resolve();
                        ClassData classData = new ClassDataImpl(typeDescription, jarSource.mod);
                        classDataList.add(classData);
                        count++;
                    }
                    PatchlibLogger.info("Discovered " + count + " classes in " + jarFile.getName());

                }
            }

        } catch (IOException ex) {
            PatchlibLogger.error("Ran in to an IOException during class loading.");
            throw new RuntimeException(ex);
        }

        return classDataList;
    }

    private List<ClassFileLocator> getLocators(List<JarSource> jars) {
        List<ClassFileLocator> locators = new ArrayList<>();
        locators.add(ClassFileLocator.ForClassLoader.ofSystemLoader()); //Required to read JVM and game Classes that appear on the annotations
        locators.add(ClassFileLocator.ForClassLoader.of(ClassDiscoverer.class.getClassLoader()));

        for (JarSource jar : jars) {
            try {
                locators.add(ClassFileLocator.ForJarFile.of(jar.jar));
            } catch (IOException ex) {
                PatchlibLogger.error("Could not add " + jar.jar.getName() + " to classfile locators");
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
            PatchlibLogger.info("Discovered jar: " + jar.getName());
        }

        //Add mod jars
        for (ModSpecAPI mod : mods) {
            for (String jarPath : mod.getJars()) {
                File jar = new File(mod.getPath(), jarPath);
                jars.add(new JarSource(mod, jar));
                PatchlibLogger.info("Discovered jar: " + jar.getName() + " from " + mod.getName());
            }
        }

        return jars;
    }

}
