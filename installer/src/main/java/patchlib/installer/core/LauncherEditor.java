package patchlib.installer.core;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Idempotent insertion of the -javaagent entry into the games launcher file. */
public final class LauncherEditor {

    private LauncherEditor() {}

    public static final String AGENT_ARG = "-javaagent:PatchLibAgent.jar";

    //The prior vmparams is copied here, next to the original, before we change it.
    public static final String BACKUP_NAME = "vmparams_patchlib_backup";

    //Matches any -javaagent token pointing at PatchLibAgent.jar, so a manual or path-prefixed entry counts as present.
    private static final Pattern EXISTING = Pattern.compile("-javaagent:\\S*PatchLibAgent\\.jar");

    //The leading java executable on the vmparams line.
    private static final Pattern JAVA_EXE = Pattern.compile("(?i)javaw?\\.exe");

    /** Ensures the Windows vmparams single line contains the agent argument. Returns true if the file changed. */
    public static boolean ensureWindows(File vmparams) throws InstallException {
        try {
            String content = new String(Files.readAllBytes(vmparams.toPath()), StandardCharsets.UTF_8);
            if (EXISTING.matcher(content).find()) {
                return false; // already present, nothing to do, so no backup either
            }
            //Save the users prior vmparams next to the original before changing anything.
            writeBackup(vmparams);
            writeAtomically(vmparams, insertWindows(content));
            return true;
        } catch (IOException e) {
            throw new InstallException("Could not update " + vmparams.getName() + ": " + e.getMessage(), e);
        }
    }

    //vmparams is a single line. Insert the agent arg right after -noverify if present, otherwise after the java executable.
    private static String insertWindows(String content) throws InstallException {
        int noverify = content.indexOf("-noverify");
        if (noverify >= 0) {
            int end = noverify + "-noverify".length();
            return content.substring(0, end) + " " + AGENT_ARG + content.substring(end);
        }
        Matcher m = JAVA_EXE.matcher(content);
        if (m.find()) {
            return content.substring(0, m.end()) + " " + AGENT_ARG + content.substring(m.end());
        }
        throw new InstallException("Could not find a place to add the loader in vmparams. The file may be corrupted.");
    }

    //Byte exact copy of the current launcher file, placed in the same folder.
    private static void writeBackup(File vmparams) throws IOException {
        File backup = new File(vmparams.getParentFile(), BACKUP_NAME);
        Files.copy(vmparams.toPath(), backup.toPath(), StandardCopyOption.REPLACE_EXISTING);
    }

    private static void writeAtomically(File target, String content) throws IOException {
        Path dir = target.getParentFile().toPath();
        Path tmp = Files.createTempFile(dir, "vmparams", ".tmp");
        Files.write(tmp, content.getBytes(StandardCharsets.UTF_8));
        try {
            Files.move(tmp, target.toPath(), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException atomicUnsupported) {
            //Some filesystems do not support an atomic move; fall back to a plain replace.
            Files.move(tmp, target.toPath(), StandardCopyOption.REPLACE_EXISTING);
        }
    }
}
