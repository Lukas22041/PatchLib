package patchlib.agent.profiler;

import jdk.jfr.Configuration;
import jdk.jfr.FlightRecorder;
import jdk.jfr.Recording;

import javax.management.ObjectName;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

/** Records the game with the JDK Flight Recorder. */
public class JfrProfiler {

    private static final DateTimeFormatter FILE_NAME = DateTimeFormatter.ofPattern("'profile_'yyyy-MM-dd'_time_'HH-mm-ss'.jfr'");
    private static final Duration SAMPLE_PERIOD = Duration.ofMillis(2);

    private static Recording recording;
    private static Path outputFile;

    public static boolean isRecording() {
        return recording != null;
    }

    public static Duration getElapsedTime() {
        return Duration.between(recording.getStartTime(), Instant.now());
    }

    /** The size is not a point at which the recording ends, the oldest data is dropped to stay below it. */
    public static void start(Path folder, long maxSizeBytes) throws Exception {
        Path absoluteFolder = folder.toAbsolutePath().normalize();
        Files.createDirectories(absoluteFolder);
        moveRepositoryIfNeeded(absoluteFolder.resolveSibling("repository"));

        Path file = absoluteFolder.resolve(LocalDateTime.now().format(FILE_NAME));

        Recording newRecording = new Recording(Configuration.getConfiguration("profile"));
        newRecording.setName("Starsector");
        newRecording.disable("jdk.InitialEnvironmentVariable");
        newRecording.disable("jdk.SystemProcess");
        newRecording.enable("jdk.ExecutionSample").withPeriod(SAMPLE_PERIOD);
        newRecording.enable("jdk.NativeMethodSample").withPeriod(SAMPLE_PERIOD);
        newRecording.enable("jdk.ClassLoad");
        newRecording.setToDisk(true);
        newRecording.setMaxSize(maxSizeBytes);
        newRecording.setDestination(file);
        newRecording.setDumpOnExit(true); //Still writes the file if the game is closed while recording
        newRecording.start();

        recording = newRecording;
        outputFile = file;
    }

    public static void setMaxSize(long maxSizeBytes) {
        if (recording != null) recording.setMaxSize(maxSizeBytes);
    }

    /** Returns the file that was written. */
    public static Path stop() throws IOException {
        Recording finished = recording;
        recording = null;

        try {
            finished.stop();
        } finally {
            finished.close();
        }

        if (Files.size(outputFile) > 0) return outputFile;

        Files.delete(outputFile);
        throw new IOException("JFR wrote an empty recording, which happens if its repository folder has non-ASCII characters in its path.");
    }

    /** Selects the latest recording in the file manager on Windows and macOS. Linux has no common way of doing this, so it just opens the folder. */
    public static void openFolder(Path folder) throws IOException {
        Path absoluteFolder = folder.toAbsolutePath().normalize();
        Files.createDirectories(absoluteFolder);

        Path latest = findLatestRecording(absoluteFolder);
        String os = System.getProperty("os.name").toLowerCase();

        List<String> command;
        if (os.contains("win")) command = latest == null ? List.of("explorer", absoluteFolder.toString()) : List.of("explorer", "/select,", latest.toString()); //Explorer ignores /select if it is quoted together with a path with spaces.
        else if (os.contains("mac")) command = latest == null ? List.of("open", absoluteFolder.toString()) : List.of("open", "-R", latest.toString());
        else command = List.of("xdg-open", absoluteFolder.toString());

        new ProcessBuilder(command).start();
    }

    private static Path findLatestRecording(Path folder) throws IOException {
        try (Stream<Path> files = Files.list(folder)) {
            return files.filter(file -> file.toString().endsWith(".jfr")).max(Comparator.comparingLong(file -> file.toFile().lastModified())).orElse(null);
        }
    }

    /** JFR writes empty recordings without an error if its repository path has non-ASCII characters, and by default it is in the temp folder, which includes the windows user name.
     * The repository is shared by all recordings in the JVM and can not be changed for free once JFR is running, so this only acts if it would fail otherwise. */
    private static void moveRepositoryIfNeeded(Path repository) throws Exception {
        if (FlightRecorder.isInitialized() || isAscii(System.getProperty("java.io.tmpdir")) || !isAscii(repository.toString())) return;

        Files.createDirectories(repository);
        String argument = "repositorypath=\"" + repository.toString().replace("\\", "\\\\") + "\"";
        ManagementFactory.getPlatformMBeanServer().invoke(
                new ObjectName("com.sun.management:type=DiagnosticCommand"),
                "jfrConfigure",
                new Object[]{ new String[]{ argument } },
                new String[]{ String[].class.getName() });
    }

    private static boolean isAscii(String text) {
        return StandardCharsets.US_ASCII.newEncoder().canEncode(text);
    }
}
