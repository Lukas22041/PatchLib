package patchlib.installer.core;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;

/** Checks, without triggering a UAC prompt, whether the install needs admin rights. */
public final class WritePermissionProbe {

    private WritePermissionProbe() {}

    /** True when either the working dir or the launcher file cannot be written without elevation. */
    public static boolean needsElevation(File workingDir, File launcherFile) {
        return !canWriteInDir(workingDir) || !canWriteFile(launcherFile);
    }

    private static boolean canWriteInDir(File dir) {
        if (dir == null || !dir.isDirectory()) {
            return false;
        }
        File probe = null;
        try {
            probe = File.createTempFile("patchlib-probe", ".tmp", dir);
            return true;
        } catch (IOException e) {
            return false;
        } finally {
            if (probe != null) {
                probe.delete();
            }
        }
    }

    //Opening for write does not change the file, but fails fast with AccessDenied when elevation is needed.
    //File.canWrite() is not used because it is unreliable under Windows UAC virtualization.
    private static boolean canWriteFile(File file) {
        if (file == null || !file.isFile()) {
            return false;
        }
        try (RandomAccessFile raf = new RandomAccessFile(file, "rw")) {
            return true;
        } catch (IOException e) {
            return false;
        }
    }
}
