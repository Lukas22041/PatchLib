package patch_lib.agent.discover;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.ModSpecAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import net.bytebuddy.dynamic.ClassFileLocator;
import net.bytebuddy.pool.TypePool;
import patch_lib.agent.PatchLibLogger;
import patch_lib.agent.data.PatchSpec;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public class PatchScanner {

    public void scan() {

        //Collect the jars of every enabled mod
        List<ModSpecAPI> enabledMods = Global.getSettings().getModManager().getEnabledModsCopy();
        List<File> jars = enabledMods.stream()
                .flatMap( spec ->
                        spec.getJars().stream()
                                .map( jar -> new File(spec.getPath(), jar)) )
                .toList();

        PatchLibLogger.debug("Starting annotation scan in the following jars: ");
        jars.forEach(jar -> PatchLibLogger.debug(" - " + jar.getPath()));

        //Create the class file locators for scanning the bytes of the classes
        List<ClassFileLocator> locators = new ArrayList<>();
        try {
            for (File jar : jars) {
                locators.add(ClassFileLocator.ForJarFile.of(jar));
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        List<PatchSpec> patches = new ArrayList<>();
        try (ClassFileLocator locator = new ClassFileLocator.Compound(locators);) {

            //Type pool for grabbing information from classes. Set to "FAST" so that method bodies are skipped in parsing.
            TypePool pool = new TypePool.Default(new TypePool.CacheProvider.Simple(), locator, TypePool.Default.ReaderMode.FAST);

            for (File jar : jars) {
                try (JarFile jarFile = new JarFile(jar)) {
                    Enumeration<JarEntry> entries = jarFile.entries();

                    //Iterate over every class in the jar
                    while (entries.hasMoreElements()) {
                        JarEntry entry = entries.nextElement();

                        //Skip none-classes
                        if (entry.isDirectory() || !entry.getName().endsWith(".class")) continue;

                    }
                }
            }

        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }

    }

}
