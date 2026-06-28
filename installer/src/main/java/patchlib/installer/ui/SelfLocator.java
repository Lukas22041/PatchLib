package patchlib.installer.ui;

import patchlib.installer.InstallerMain;

import java.io.File;
import java.net.URISyntaxException;

/** Finds the installer jar on disk so it can be relaunched with elevation. */
final class SelfLocator {

    private SelfLocator() {}

    static File installerJar() {
        try {
            File f = new File(InstallerMain.class.getProtectionDomain().getCodeSource().getLocation().toURI());
            return f.isFile() ? f : null;
        } catch (URISyntaxException | RuntimeException e) {
            return null;
        }
    }
}
