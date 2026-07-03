package patchlib.agent;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/** Small helper for listing the classes inside a jar. */
public final class JarClasses {

    private JarClasses() {}

    /** The binary names (package format) of all classes in the jar, skipping non-class entries. */
    public static List<String> namesIn(JarFile jarFile) {
        List<String> names = new ArrayList<>();
        Enumeration<JarEntry> entries = jarFile.entries();
        while (entries.hasMoreElements()) {
            JarEntry entry = entries.nextElement();
            if (entry.isDirectory() || !entry.getName().endsWith(".class")) continue;

            String name = entry.getName();
            names.add(name.substring(0, name.length() - ".class".length()).replace('/', '.'));
        }
        return names;
    }
}
