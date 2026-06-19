package patch_lib.installer.platform;

import patch_lib.installer.InstallArgs;
import patch_lib.installer.core.AgentJarCopier;
import patch_lib.installer.core.ApplyResult;
import patch_lib.installer.core.InstallException;
import patch_lib.installer.core.InstallValidator;
import patch_lib.installer.core.LauncherEditor;

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
