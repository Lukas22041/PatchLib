package patchlib.install;

import com.fs.starfarer.api.Global;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/** Launches the installer with the games bundled JRE and quits the game so the files can be rewritten. */
public final class InstallerLauncher {

    private InstallerLauncher() {}

    public enum Reason { MISSING_AGENT, VERSION_MISMATCH }

    /** Starts the installer for the given reason, then quits the game. Does not return on success. */
    public static void launchAndExit(Reason reason, String modVersion, String agentVersion) {
        try {
            String sep = System.getProperty("file.separator");
            String workingDir = System.getProperty("user.dir");
            String modPath = Global.getSettings().getModManager().getModSpec("patchlib").getPath();

            String os = System.getProperty("os.name", "").toLowerCase();

            String bundledJava = bundledJava(workingDir, sep);
            String installerJar = modPath + sep + "jars" + sep + "PatchLibInstaller.jar";
            String agentJar = modPath + sep + "jars" + sep + "PatchLibAgent.jar";

            List<String> cmd = new ArrayList<>();
            //The installer must keep running after the game exits via System.exit below. ProcessBuilder
            //does not detach the child, so on Linux it is torn down with the game's session/process group
            //before it can show its window. setsid starts it in a new session so it survives the exit.
            //(Windows children already survive their parent; mac uses fork mode, handled below.)
            if (os.contains("linux")) {
                cmd.add("setsid");
            }
            cmd.add(bundledJava);
            cmd.add("-jar");
            cmd.add(installerJar);
            cmd.add("--workingDir");   cmd.add(workingDir);
            cmd.add("--agentJar");     cmd.add(agentJar);
            cmd.add("--reason");       cmd.add(reason.name());
            cmd.add("--modVersion");   cmd.add(modVersion == null ? "unknown" : modVersion);
            cmd.add("--agentVersion"); cmd.add(agentVersion == null ? "none" : agentVersion);

            //macOS ships Contents/Home/lib/jspawnhelper without its exec bit, so Java's default
            //posix_spawn launch path cannot run the trampoline and the spawn fails with a misleading
            //"posix_spawn failed" against the java path. fork() execs the child directly with no helper.
            //The value is read once when ProcessImpl is first loaded, so it must be set before the JVMs
            //first spawn; PatchLib launches early enough that this is that first spawn.
            if (os.contains("mac") || os.contains("darwin")) {
                System.setProperty("jdk.lang.Process.launchMechanism", "fork");
            }

            try {
                new ProcessBuilder(cmd).start();
            } catch (IOException missingSetsid) {
                //A minimal Linux install without setsid; fall back to a direct launch. The installer
                //may not survive the game exit there, but a launch attempt beats failing outright.
                if (!cmd.isEmpty() && "setsid".equals(cmd.get(0))) {
                    cmd.remove(0);
                    new ProcessBuilder(cmd).start();
                } else {
                    throw missingSetsid;
                }
            }
        } catch (Exception e) {
            //If the installer cannot be started, fall back to a clear error so the user is never left guessing.
            throw new RuntimeException(manualMessage(reason, modVersion, agentVersion), e);
        }
        //Quit so the agent jar and launcher file are not held open while the installer rewrites them.
        System.exit(0);
    }

    //Resolves the games bundled java as a string path, not via java.home, so it points at the game JRE
    //even during dev runs.
    private static String bundledJava(String workingDir, String sep) {
        String os = System.getProperty("os.name", "").toLowerCase();
        if (os.contains("win")) {
            //workingDir is <gameRoot>/starsector-core; the jre sits in <gameRoot>. javaw avoids a console window.
            return parentOf(workingDir, sep) + sep + "jre" + sep + "bin" + sep + "javaw.exe";
        }
        if (os.contains("mac") || os.contains("darwin")) {
            //workingDir is <app>/Contents/Resources/Java; java is in <app>/Contents/Home.
            String contents = parentOf(parentOf(workingDir, sep), sep);
            return contents + sep + "Home" + sep + "bin" + sep + "java";
        }
        //Linux: workingDir is the game root.
        return workingDir + sep + "jre_linux" + sep + "bin" + sep + "java";
    }

    //Parent directory of a path, by trimming the last separated segment.
    private static String parentOf(String path, String sep) {
        int idx = path.lastIndexOf(sep);
        return idx < 0 ? path : path.substring(0, idx);
    }

    private static String manualMessage(Reason reason, String modVersion, String agentVersion) {
        String base = (reason == Reason.VERSION_MISMATCH)
            ? "PatchLib's loader is out of date (mod " + modVersion + ", loader " + agentVersion + ")."
            : "PatchLib could not find its loader.";
        return base + " The installer could not be started automatically. "
            + "Check PatchLib's forum thread for manual installation steps.";
    }
}
