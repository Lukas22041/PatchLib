package patchlib.installer;

import patchlib.installer.core.ApplyResult;
import patchlib.installer.core.ResultFile;
import patchlib.installer.platform.Platform;
import patchlib.installer.platform.PlatformFactory;
import patchlib.installer.ui.InstallerDialog;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;

/** Entry point. Shows the UI normally, or performs the install headless when relaunched with --apply. */
public final class InstallerMain {

    public static void main(String[] argv) {
        InstallArgs args;
        try {
            args = InstallArgs.parse(argv);
        } catch (RuntimeException e) {
            System.err.println("PatchLib installer: " + e.getMessage());
            System.exit(2);
            return;
        }

        if (args.apply) {
            runApply(args);
        } else {
            runUi(args);
        }
    }

    //Headless install, run inside the elevated process. Reports via the result file and the exit code.
    private static void runApply(InstallArgs args) {
        Platform platform = PlatformFactory.current();
        ApplyResult result;
        try {
            result = platform.apply(args);
        } catch (Throwable t) {
            result = ApplyResult.failure("Install failed: " + t.getMessage());
        }
        ResultFile.write(args.resultFile, result);
        System.exit(result.success ? 0 : 1);
    }

    private static void runUi(InstallArgs args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {
            //Keep the default look and feel if the system one is unavailable.
        }
        SwingUtilities.invokeLater(() -> new InstallerDialog(args).setVisible(true));
    }
}
