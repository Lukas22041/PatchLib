package patchlib.installer.core;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.PosixFilePermission;
import java.util.Set;
import java.util.regex.Pattern;

/** Idempotent insertion of the -javaagent entry into a Unix shell launcher (Mac and Linux).
 * The launcher is a multi line script that uses backslash continuations, so the agent argument is
 * added as its own continuation line right after the -noverify line, leaving the rest byte for byte. */
public final class ShellLauncherEditor {

    private ShellLauncherEditor() {}

    //Same loader argument the Windows editor inserts, kept as the single source of truth.
    public static final String AGENT_ARG = LauncherEditor.AGENT_ARG;

    //The prior launcher is copied to "<name>_patchlib_backup", next to the original, before we change it.
    public static final String BACKUP_SUFFIX = "_patchlib_backup";

    //Matches any -javaagent token pointing at PatchLibAgent.jar, so a manual or path-prefixed entry counts as present.
    private static final Pattern EXISTING = Pattern.compile("-javaagent:\\S*PatchLibAgent\\.jar");

    //The continuation flag every shipped Starsector launch script carries; we hang the new line off it.
    private static final String ANCHOR = "-noverify";

    /** Ensures the shell launcher contains the agent argument. Returns true if the file changed. */
    public static boolean ensure(File launcher) throws InstallException {
        try {
            String content = new String(Files.readAllBytes(launcher.toPath()), StandardCharsets.UTF_8);
            if (EXISTING.matcher(content).find()) {
                return false; // already present, nothing to do, so no backup either
            }
            //Build the new content first; if the anchor is missing this throws before we touch any file.
            String updated = insert(content);
            //Save the users prior launcher next to the original before changing anything.
            writeBackup(launcher);
            writeAtomicallyKeepingPermissions(launcher, updated);
            return true;
        } catch (IOException e) {
            throw new InstallException("Could not update " + launcher.getName() + ": " + e.getMessage(), e);
        }
    }

    //Inserts the agent argument as a new continuation line right after the -noverify line, copying that
    //line's indentation and the file's newline style so nothing else in the script shifts.
    private static String insert(String content) throws InstallException {
        int anchor = content.indexOf(ANCHOR);
        if (anchor < 0) {
            throw cannotPlace();
        }
        int lineEnd = content.indexOf('\n', anchor);
        if (lineEnd < 0) {
            //The -noverify line has no following newline, so the script is not shaped the way we expect.
            throw cannotPlace();
        }
        //The -noverify line must be backslash continued; otherwise a continued line added after it would
        //graft the agent argument onto a command that had already ended, breaking the script.
        if (!content.substring(anchor, lineEnd).trim().endsWith("\\")) {
            throw cannotPlace();
        }
        boolean crlf = lineEnd > 0 && content.charAt(lineEnd - 1) == '\r';
        String newline = crlf ? "\r\n" : "\n";

        //Reuse only the leading whitespace of the -noverify line, never any token that shares it, so the
        //new line lines up without duplicating anything.
        int lineStart = content.lastIndexOf('\n', anchor) + 1; // 0 when -noverify is on the first line
        String indent = leadingWhitespace(content, lineStart);

        String inserted = indent + AGENT_ARG + " \\" + newline;
        return content.substring(0, lineEnd + 1) + inserted + content.substring(lineEnd + 1);
    }

    //The run of spaces and tabs at the start of the line beginning at "from".
    private static String leadingWhitespace(String content, int from) {
        int i = from;
        while (i < content.length() && (content.charAt(i) == ' ' || content.charAt(i) == '\t')) {
            i++;
        }
        return content.substring(from, i);
    }

    private static InstallException cannotPlace() {
        return new InstallException(
            "Could not find a place to add the loader in the launcher script. The file may be corrupted.");
    }

    //Byte exact copy of the current launcher, placed in the same folder, keeping its attributes.
    private static void writeBackup(File launcher) throws IOException {
        File backup = new File(launcher.getParentFile(), launcher.getName() + BACKUP_SUFFIX);
        Files.copy(launcher.toPath(), backup.toPath(),
            StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.COPY_ATTRIBUTES);
    }

    //Writes via a temp file and an atomic move so a failure never leaves a half written launcher.
    //A fresh temp file is created with restrictive permissions, so the original mode is copied onto it
    //before the move; otherwise the launcher would lose its executable bit and the game could not start.
    private static void writeAtomicallyKeepingPermissions(File target, String content) throws IOException {
        Path targetPath = target.toPath();
        Set<PosixFilePermission> perms = readPosixPermissions(targetPath);

        Path dir = target.getParentFile().toPath();
        Path tmp = Files.createTempFile(dir, target.getName(), ".tmp");
        try {
            Files.write(tmp, content.getBytes(StandardCharsets.UTF_8));
            if (perms != null) {
                try {
                    Files.setPosixFilePermissions(tmp, perms);
                } catch (UnsupportedOperationException | IOException ignored) {
                    //Non POSIX filesystem; Mac and Linux are POSIX, so this should not happen there.
                }
            }
            try {
                Files.move(tmp, targetPath, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (IOException atomicUnsupported) {
                //Some filesystems do not support an atomic move; fall back to a plain replace.
                Files.move(tmp, targetPath, StandardCopyOption.REPLACE_EXISTING);
            }
        } finally {
            //If the move already happened this is a no op; otherwise it clears the leftover temp file.
            Files.deleteIfExists(tmp);
        }
    }

    private static Set<PosixFilePermission> readPosixPermissions(Path path) {
        try {
            return Files.getPosixFilePermissions(path);
        } catch (UnsupportedOperationException | IOException e) {
            return null;
        }
    }
}
