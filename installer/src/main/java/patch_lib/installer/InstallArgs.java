package patch_lib.installer;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/** Parsed command line arguments, shared by the UI process and the elevated apply process. */
public final class InstallArgs {

    public final boolean apply;
    public final File workingDir;
    public final File agentJar;
    public final Reason reason;
    public final String modVersion;
    public final String agentVersion;
    public final File resultFile;

    private InstallArgs(boolean apply, File workingDir, File agentJar, Reason reason,
                        String modVersion, String agentVersion, File resultFile) {
        this.apply = apply;
        this.workingDir = workingDir;
        this.agentJar = agentJar;
        this.reason = reason;
        this.modVersion = modVersion;
        this.agentVersion = agentVersion;
        this.resultFile = resultFile;
    }

    public static InstallArgs parse(String[] argv) {
        boolean apply = false;
        String workingDir = null;
        String agentJar = null;
        String reason = null;
        String modVersion = null;
        String agentVersion = null;
        String resultFile = null;

        for (int i = 0; i < argv.length; i++) {
            switch (argv[i]) {
                case "--apply":        apply = true; break;
                case "--workingDir":   workingDir = next(argv, ++i); break;
                case "--agentJar":     agentJar = next(argv, ++i); break;
                case "--reason":       reason = next(argv, ++i); break;
                case "--modVersion":   modVersion = next(argv, ++i); break;
                case "--agentVersion": agentVersion = next(argv, ++i); break;
                case "--resultFile":   resultFile = next(argv, ++i); break;
                default: break; // ignore unknown tokens
            }
        }

        if (workingDir == null) {
            throw new IllegalArgumentException("Missing --workingDir");
        }
        if (agentJar == null) {
            throw new IllegalArgumentException("Missing --agentJar");
        }

        return new InstallArgs(
            apply,
            new File(workingDir),
            new File(agentJar),
            Reason.fromName(reason),
            modVersion,
            agentVersion,
            resultFile == null ? null : new File(resultFile)
        );
    }

    private static String next(String[] argv, int i) {
        if (i >= argv.length) {
            throw new IllegalArgumentException("Missing value for " + argv[i - 1]);
        }
        return argv[i];
    }

    /** Builds the argument list for the elevated apply process (everything after "-jar &lt;jar&gt;"). */
    public List<String> toApplyArgs(File resultFile) {
        List<String> args = new ArrayList<>();
        args.add("--apply");
        args.add("--workingDir");   args.add(workingDir.getAbsolutePath());
        args.add("--agentJar");     args.add(agentJar.getAbsolutePath());
        args.add("--reason");       args.add(reason.name());
        args.add("--modVersion");   args.add(modVersion == null ? "unknown" : modVersion);
        args.add("--agentVersion"); args.add(agentVersion == null ? "none" : agentVersion);
        args.add("--resultFile");   args.add(resultFile.getAbsolutePath());
        return args;
    }
}
