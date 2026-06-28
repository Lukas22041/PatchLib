package patchlib.installer.platform;

import patchlib.installer.InstallArgs;
import patchlib.installer.core.AgentJarCopier;
import patchlib.installer.core.ApplyResult;
import patchlib.installer.core.InstallException;
import patchlib.installer.core.InstallValidator;
import patchlib.installer.core.LauncherEditor;

import java.io.File;

/** Windows install: drop the agent jar into starsector-core and add the loader to vmparams. */
public final class WindowsPlatform implements Platform {

    @Override
    public boolean supportsAutoInstall() {
        return true;
    }

    @Override
    public boolean supportsElevation() {
        //Windows installs usually live under Program Files, so a UAC prompt may be needed.
        return true;
    }

    @Override
    public File launcherFile(File workingDir) {
        //workingDir is <gameRoot>/starsector-core; vmparams sits one level up in <gameRoot>.
        return new File(workingDir.getParentFile(), "vmparams");
    }

    @Override
    public ApplyResult apply(InstallArgs args) {
        File workingDir = args.workingDir;
        File vmparams = launcherFile(workingDir);
        try {
            InstallValidator.validate(workingDir, vmparams);
            //Copy the jar first so the launcher never points at a missing file.
            AgentJarCopier.copy(args.agentJar, new File(workingDir, "PatchLibAgent.jar"));
            LauncherEditor.ensureWindows(vmparams);
            return ApplyResult.ok("Install complete. You can start Starsector again now.");
        } catch (InstallException e) {
            return ApplyResult.failure(e.getMessage());
        }
    }
}
