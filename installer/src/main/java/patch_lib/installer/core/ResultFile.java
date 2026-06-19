package patch_lib.installer.core;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

/** Carries the apply outcome from the elevated process back to the UI process. */
public final class ResultFile {

    private ResultFile() {}

    private static final String OK = "OK";
    private static final String FAIL = "FAIL";

    public static void write(File file, ApplyResult result) {
        if (file == null) {
            return;
        }
        String status = result.success ? OK : FAIL;
        String text = status + "\n" + (result.message == null ? "" : result.message);
        try {
            Files.write(file.toPath(), text.getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            //Nothing we can do here; the parent process falls back to the exit code.
        }
    }

    public static ApplyResult read(File file, int exitCode) {
        if (file != null && file.isFile()) {
            try {
                String text = new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
                int nl = text.indexOf('\n');
                String status = (nl < 0 ? text : text.substring(0, nl)).trim();
                String message = (nl < 0 ? "" : text.substring(nl + 1)).trim();
                if (OK.equals(status)) {
                    return ApplyResult.ok(message.isEmpty() ? "Install complete." : message);
                }
                return ApplyResult.failure(message.isEmpty() ? "Install failed." : message);
            } catch (IOException e) {
                //Fall through to the exit code.
            }
        }
        return exitCode == 0
            ? ApplyResult.ok("Install complete.")
            : ApplyResult.failure("Install failed (code " + exitCode + ").");
    }
}
