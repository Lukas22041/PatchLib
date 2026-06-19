package patch_lib.installer.platform;

import patch_lib.installer.InstallArgs;
import patch_lib.installer.core.ApplyResult;

import java.io.File;

/** Per OS behaviour. Windows, Mac and Linux are all implemented.
 * The path logic here must stay in sync with starsectorLayout() in the root build.gradle.kts. */
public interface Platform {

    /** Whether the Install button is enabled. True on every supported platform. */
    boolean supportsAutoInstall();

    /** Whether the installer can relaunch itself with elevated rights to gain write access.
     * Only Windows can (via a UAC prompt); Mac and Linux installs live in user owned folders, so
     * there the install is simply attempted and any write failure is reported as is. */
    boolean supportsElevation();

    /** The launcher file that holds the JVM arguments, derived from the games working directory. */
    File launcherFile(File workingDir);

    /** Runs the actual install: validate, copy the agent jar, and make sure the launcher loads it. */
    ApplyResult apply(InstallArgs args);
}
