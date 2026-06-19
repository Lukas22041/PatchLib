package patch_lib.installer.core;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

/** Copies the shipped agent jar into the games working directory. */
public final class AgentJarCopier {

    private AgentJarCopier() {}

    private static final int ATTEMPTS = 5;
    private static final long RETRY_DELAY_MS = 200;

    public static void copy(File source, File dest) throws InstallException {
        if (source == null || !source.isFile() || source.length() == 0) {
            throw new InstallException("The agent jar to install was not found at " + source + ".");
        }

        IOException last = null;
        for (int attempt = 0; attempt < ATTEMPTS; attempt++) {
            try {
                Files.copy(source.toPath(), dest.toPath(), StandardCopyOption.REPLACE_EXISTING);
                return;
            } catch (IOException e) {
                //The game may still hold the old jar for a moment after it quit. Wait briefly and retry.
                last = e;
                sleep(RETRY_DELAY_MS);
            }
        }
        throw new InstallException(
            "Could not copy the agent jar into " + dest.getParent() + ": " + last.getMessage(), last);
    }

    private static void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
