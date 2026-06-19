package patch_lib.installer.core;

import java.io.File;

/** Confirms the target really is a Starsector install before anything is written. */
public final class InstallValidator {

    private InstallValidator() {}

    public static void validate(File workingDir, File launcherFile) throws InstallException {
        if (launcherFile == null || !launcherFile.isFile()) {
            throw new InstallException("Could not find the launcher file at " + launcherFile + ".");
        }
        //starfarer.api.jar is the stable API jar that ships in every Starsector working dir and is not
        //replaced on updates, so it is a reliable marker that this really is a Starsector install.
        File marker = new File(workingDir, "starfarer.api.jar");
        if (!marker.isFile()) {
            throw new InstallException(
                "Expected to find starfarer.api.jar in " + workingDir + ".");
        }
    }
}
