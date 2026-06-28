package patchlib.installer.platform;

import patchlib.installer.InstallArgs;
import patchlib.installer.core.AgentJarCopier;
import patchlib.installer.core.ApplyResult;
import patchlib.installer.core.InstallException;
import patchlib.installer.core.InstallValidator;
import patchlib.installer.core.ShellLauncherEditor;

import java.io.File;

/** Mac install: drop the agent jar into Contents/Resources/Java and add the loader to starsector_mac.sh. */
public final class MacPlatform implements Platform {

    @Override
    public boolean supportsAutoInstall() {
        return true;
    }

    @Override
    public boolean supportsElevation() {
        //Mac installs are extracted wherever the user likes, so they own the files; no elevation.
        return false;
    }

    @Override
    public File launcherFile(File workingDir) {
        //workingDir is <app>/Contents/Resources/Java; the launcher script is in <app>/Contents/MacOS.
        File contents = workingDir.getParentFile().getParentFile();
        return new File(contents, "MacOS/starsector_mac.sh");
    }

    @Override
    public ApplyResult apply(InstallArgs args) {
        File workingDir = args.workingDir;
        File launcher = launcherFile(workingDir);
        try {
            InstallValidator.validate(workingDir, launcher);
            //Copy the jar first so the launcher never points at a missing file. The mac script changes
            //into Contents/Resources/Java before launching, so the agent jar belongs there next to it.
            AgentJarCopier.copy(args.agentJar, new File(workingDir, "PatchLibAgent.jar"));
            ShellLauncherEditor.ensure(launcher);
            return ApplyResult.ok("Install complete. You can start Starsector again now.");
        } catch (InstallException e) {
            return ApplyResult.failure(e.getMessage());
        }
    }
}
