package patchlib.agent.matchers;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.ModSpecAPI;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.ClassFileLocator;
import net.bytebuddy.matcher.ElementMatcher;
import net.bytebuddy.pool.TypePool;
import patchlib.agent.PatchLibLogger;
import patchlib.agent.spec.PatchSpec;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import static net.bytebuddy.matcher.ElementMatchers.hasSuperType;

/** A class that builds an index of all relevant subtypes for the subtype argument matcher.
 * This can be used by the GateMatcher to quickly gate out any irrelevant classes for patches that use the subtype matcher.
 * The main reason for this was due to stutter during gameplay, where newly loaded classes (mostly from UI) caused a bunch of lag during their transform. */
public final class SubtypeIndex {

    private final Set<String> matches;

    private SubtypeIndex(Set<String> matches) {
        this.matches = matches;
    }

    public boolean contains(String className) {
        return matches.contains(className);
    }

    public static SubtypeIndex build(List<PatchSpec> patches) {
        Set<String> matches = new HashSet<>();
        Set<String> targetSubtypes = collectSubtypeTargets(patches);
        if (targetSubtypes.isEmpty()) return new SubtypeIndex(matches);

        List<File> jars = gatherScannableJars();

        ElementMatcher.Junction<TypeDescription> isSubtype = hasSuperType(type -> targetSubtypes.contains(type.getActualName()));

        List<ClassFileLocator> locators = new ArrayList<>();
        locators.add(ClassFileLocator.ForClassLoader.ofSystemLoader());
        for (File jar : jars) {
            try {
                locators.add(ClassFileLocator.ForJarFile.of(jar));
            } catch (IOException ex) {
                PatchLibLogger.error("Could not open jar for subtype indexing: " + jar);
            }
        }

        try (ClassFileLocator locator = new ClassFileLocator.Compound(locators)) {
            TypePool pool = new TypePool.Default(new TypePool.CacheProvider.Simple(), locator, TypePool.Default.ReaderMode.FAST);

            for (File jar : jars) {
                if (!jar.isFile()) continue;
                try (JarFile jarFile = new JarFile(jar)) {
                    Enumeration<JarEntry> entries = jarFile.entries();
                    while (entries.hasMoreElements()) {
                        JarEntry entry = entries.nextElement();
                        if (entry.isDirectory() || !entry.getName().endsWith(".class")) continue;

                        String binaryName = entry.getName()
                                .substring(0, entry.getName().length() - ".class".length())
                                .replace('/', '.');

                        if (IgnoreMatcher.isIgnored(binaryName)) continue;

                        try {
                            TypeDescription type = pool.describe(binaryName).resolve();
                            if (isSubtype.matches(type)) matches.add(type.getName());
                        } catch (Exception ignored) { }
                    }
                } catch (IOException ex) {
                    PatchLibLogger.error("Could not scan jar for subtype indexing: " + jar);
                }
            }
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }

        PatchLibLogger.info("Subtype index built with " + matches.size() + " matching classes for " + targetSubtypes.size() + " target(s)");
        return new SubtypeIndex(matches);
    }

    /** Scans for working directory jars, i.e starsector itself and scans for mod jars */
    private static List<File> gatherScannableJars() {
        List<File> jars = new ArrayList<>();

        File[] coreJars = new File(System.getProperty("user.dir")).listFiles((dir, name) -> name.endsWith(".jar"));
        if (coreJars != null) {
            for (File jar : coreJars) jars.add(jar);
        }

        for (ModSpecAPI mod : Global.getSettings().getModManager().getEnabledModsCopy()) {
            for (String jar : mod.getJars()) jars.add(new File(mod.getPath(), jar));
        }
        return jars;
    }


    private static Set<String> collectSubtypeTargets(List<PatchSpec> specs) {
        Set<String> targets = new HashSet<>();
        for (PatchSpec spec : specs) {
            String subtype = spec.targetClass().targetSubtype();
            if (!subtype.isEmpty()) targets.add(subtype);
        }
        return targets;
    }
}
