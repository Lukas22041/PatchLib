package patchlib.agent.patch;

import patchlib.agent.PatchLibLogger;

import java.io.File;
import java.io.IOException;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public final class StarsectorPreloader {

    private static final String[] GAME_JARS = { "starfarer_obf.jar", "starfarer.api.jar" };

    private StarsectorPreloader() {}

    /** Force-loads all game classes. Extends loading by around 0.5-1.5 seconds and increases ram usage by around 30mb
     * This ram usage would however also be loaded in soon anyhow. What this preload accomplishes is
     * making the expensive patch matching & transformation happen during load time, rather than while a player could feel any stutter. */
    public static void preload(ClassLoader loader) {
        PatchLibLogger.info("Starting game class preload.");
        long start = System.nanoTime();

        int loaded = 0;
        int skipped = 0;

        for (String jarName : GAME_JARS) {
            File jar = new File(System.getProperty("user.dir"), jarName);
            if (!jar.isFile()) {
                PatchLibLogger.warn("Preloader: " + jarName + " not found, skipping");
                continue;
            }

            try (JarFile jarFile = new JarFile(jar)) {
                Enumeration<JarEntry> entries = jarFile.entries();
                while (entries.hasMoreElements()) {
                    JarEntry entry = entries.nextElement();
                    if (entry.isDirectory() || !entry.getName().endsWith(".class")) continue;

                    String name = entry.getName()
                            .substring(0, entry.getName().length() - ".class".length())
                            .replace('/', '.');

                    try {
                        //Loads the class, which immediately forces it through the retransformation from the bytebuddy agent.
                        //"Initialize" set to false, as a wrong order of static block executions could have side effects.
                        Class.forName(name, false, loader);
                        loaded++;
                    } catch (Throwable t) {
                        skipped++; //cant be linked this early or is not a real class
                    }
                }
            } catch (IOException ex) {
                PatchLibLogger.error("Preloader could not read " + jarName + ": " + ex);
            }
        }

        long ms = (System.nanoTime() - start) / 1000000;
        PatchLibLogger.info("Preloaded " + loaded + " game classes (" + skipped + " skipped) in " + ms + " ms");
    }

}
