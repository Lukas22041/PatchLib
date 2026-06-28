package patchlib.installer.platform;

import patchlib.installer.InstallArgs;
import patchlib.installer.core.AgentJarCopier;
import patchlib.installer.core.ApplyResult;
import patchlib.installer.core.InstallException;
import patchlib.installer.core.InstallValidator;
import patchlib.installer.core.ShellLauncherEditor;

import java.io.File;

/** Linux install: drop the agent jar into the game root and add the loader to starsector.sh. */
public final class LinuxPlatform implements Platform {

    @Override
    public boolean supportsAutoInstall() {
        return true;
    }

    @Override
    public boolean supportsElevation() {
        //Linux installs are extracted wherever the user likes, so they own the files; no elevation.
        return false;
    }

    @Override
    public File launcherFile(File workingDir) {
        //workingDir is the game root on Linux.
        return new File(workingDir, "starsector.sh");
    }

    @Override
    public ApplyResult apply(InstallArgs args) {
        File workingDir = args.workingDir;
        File launcher = launcherFile(workingDir);
        try {
            InstallValidator.validate(workingDir, launcher);
            //Copy the jar first so the launcher never points at a missing file.
            AgentJarCopier.copy(args.agentJar, new File(workingDir, "PatchLibAgent.jar"));
            ShellLauncherEditor.ensure(launcher);
            return ApplyResult.ok("Install complete. You can start Starsector again now.");
        } catch (InstallException e) {
            return ApplyResult.failure(e.getMessage());
        }
    }
}
