package patch_lib.installer.platform;

/** Picks the implementation for the running OS, mirroring currentPlatform() in the root build.gradle.kts. */
public final class PlatformFactory {

    private PlatformFactory() {}

    public static Platform current() {
        String os = System.getProperty("os.name", "").toLowerCase();
        if (os.contains("win")) {
            return new WindowsPlatform();
        }
        if (os.contains("mac") || os.contains("darwin")) {
            return new MacPlatform();
        }
        return new LinuxPlatform();
    }
}
