package patch_lib.agent.discover;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.ModSpecAPI;
import net.bytebuddy.dynamic.ClassFileLocator;
import net.bytebuddy.pool.TypePool;
import patch_lib.agent.PatchLibLogger;
import patch_lib.agent.spec.PatchSpec;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public class PatchScanner {

    private static final String PATCH = "patch_lib.api.Patch";
    private static final String BEFORE = "patch_lib.api.Before";
    private static final String AFTER = "patch_lib.api.After";
    private static final String UNSET = "patch_lib.api.Unset";

    record JarPair(ModSpecAPI mod, File jar) { }

    public void scan() {

        //Collect the jars of every enabled mod
        List<ModSpecAPI> enabledMods = Global.getSettings().getModManager().getEnabledModsCopy();
        List<JarPair> jarPairs = enabledMods.stream()
                .flatMap( spec ->
                        spec.getJars().stream()
                                .map( jar -> new JarPair(spec, new File(spec.getPath(), jar))) )
                .toList();

        PatchLibLogger.debug("Starting annotation scan in the following jars: ");
        jarPairs.forEach(jar -> PatchLibLogger.debug(" - " + jar.jar.getPath()));

        //Create the class file locators for scanning the bytes of the classes
        List<ClassFileLocator> locators = new ArrayList<>();
        try {
            for (JarPair jarPair : jarPairs) {
                locators.add(ClassFileLocator.ForJarFile.of(jarPair.jar));
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        List<PatchSpec> patches = new ArrayList<>();
        try (ClassFileLocator locator = new ClassFileLocator.Compound(locators);) {

            //Type pool for grabbing information from classes. Set to "FAST" so that method bodies are skipped in parsing.
            TypePool pool = new TypePool.Default(new TypePool.CacheProvider.Simple(), locator, TypePool.Default.ReaderMode.FAST);

            for (JarPair jarPair : jarPairs) {
                try (JarFile jarFile = new JarFile(jarPair.jar)) {
                    Enumeration<JarEntry> entries = jarFile.entries();

                    //Iterate over every class in the jar
                    while (entries.hasMoreElements()) {
                        JarEntry entry = entries.nextElement();

                        //Skip non-classes
                        if (entry.isDirectory() || !entry.getName().endsWith(".class")) continue;

                    }
                }
            }

        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }

    }

}
