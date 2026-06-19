package patch_lib.installer.elevation;

import com.sun.jna.Native;
import com.sun.jna.platform.win32.Kernel32;
import com.sun.jna.platform.win32.Shell32;
import com.sun.jna.platform.win32.ShellAPI;
import com.sun.jna.platform.win32.WinBase;
import com.sun.jna.platform.win32.WinNT;
import com.sun.jna.ptr.IntByReference;

import patch_lib.installer.InstallArgs;
import patch_lib.installer.core.ApplyResult;
import patch_lib.installer.core.ResultFile;

import java.io.File;
import java.io.IOException;

/** Relaunches the installer elevated on Windows via ShellExecuteEx "runas", waits, then reads the result. */
public final class Elevation {

    private Elevation() {}

    private static final int SEE_MASK_NOCLOSEPROCESS = 0x00000040;
    private static final int SW_SHOWNORMAL = 1;
    private static final int ERROR_CANCELLED = 1223;

    /** Runs the install in an elevated child process and returns its result. */
    public static ApplyResult runElevated(InstallArgs args, File installerJar) {
        File resultFile;
        try {
            resultFile = File.createTempFile("patchlib-install-result", ".txt");
            resultFile.delete(); // the child process creates it
        } catch (IOException e) {
            return ApplyResult.failure("Could not prepare the install: " + e.getMessage());
        }

        //The installer is already running on the games bundled JRE, so java.home points at it.
        //javaw avoids a console window flashing for the headless child.
        File javaw = new File(System.getProperty("java.home"), "bin/javaw.exe");

        ShellAPI.SHELLEXECUTEINFO sei = new ShellAPI.SHELLEXECUTEINFO();
        sei.cbSize = sei.size();
        sei.fMask = SEE_MASK_NOCLOSEPROCESS;
        sei.lpVerb = "runas";
        sei.lpFile = javaw.getAbsolutePath();
        sei.lpParameters = buildParameters(installerJar, args, resultFile);
        sei.lpDirectory = args.workingDir.getAbsolutePath();
        sei.nShow = SW_SHOWNORMAL;

        if (!Shell32.INSTANCE.ShellExecuteEx(sei)) {
            int err = Native.getLastError();
            if (err == ERROR_CANCELLED) {
                return ApplyResult.cancelled();
            }
            return ApplyResult.failure("Could not get admin rights to install (error " + err + ").");
        }

        int exitCode = waitForExit(sei.hProcess);
        return ResultFile.read(resultFile, exitCode);
    }

    private static int waitForExit(WinNT.HANDLE process) {
        if (process == null) {
            return -1;
        }
        Kernel32.INSTANCE.WaitForSingleObject(process, WinBase.INFINITE);
        IntByReference code = new IntByReference();
        int exitCode = Kernel32.INSTANCE.GetExitCodeProcess(process, code) ? code.getValue() : -1;
        Kernel32.INSTANCE.CloseHandle(process);
        return exitCode;
    }

    //Builds the quoted argument string for the elevated java process. Each path is double-quoted so
    //spaces and parentheses (Program Files (x86)) survive.
    private static String buildParameters(File installerJar, InstallArgs args, File resultFile) {
        StringBuilder sb = new StringBuilder();
        sb.append("-jar ").append(quote(installerJar.getAbsolutePath()));
        for (String arg : args.toApplyArgs(resultFile)) {
            sb.append(' ');
            //Flags pass through as is; values are quoted to survive spaces.
            sb.append(arg.startsWith("--") ? arg : quote(arg));
        }
        return sb.toString();
    }

    private static String quote(String s) {
        return "\"" + s + "\"";
    }
}
