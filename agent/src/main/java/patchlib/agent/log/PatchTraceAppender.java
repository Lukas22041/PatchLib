package patchlib.agent.log;

import com.fs.starfarer.api.Global;
import org.apache.log4j.AppenderSkeleton;
import org.apache.log4j.Layout;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.spi.LoggingEvent;
import org.apache.log4j.spi.ThrowableInformation;
import patchlib.agent.patch.InstallationData;
import patchlib.agent.patch.PatchedClassRegistry;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Logs a note below logged stack traces that contain patched classes. */
public class PatchTraceAppender extends AppenderSkeleton {

    private static final Logger log = Global.getLogger(PatchTraceAppender.class);
    private static final String PREFIX = "[PatchLib] ";

    static {
        log.setLevel(Level.ALL);
    }

    /** Added to the root logger after the games own appenders, so that it runs after the stack trace was already written to the console and starsector.log. */
    public static void install() {
        Logger.getRootLogger().addAppender(new PatchTraceAppender());
    }

    @Override
    protected void append(LoggingEvent event) {
        //Only events with a throwable print a stack trace. The note itself is logged without one, so it can not trigger itself.
        ThrowableInformation throwableInformation = event.getThrowableInformation();
        if (throwableInformation == null) return;

        String note = createNote(throwableInformation.getThrowable());
        //Uses the level of the logged exception, so that the note shows up wherever the stack trace does.
        if (note != null) log.log(event.getLevel(), note);
    }

    private String createNote(Throwable throwable) {
        Map<String, Set<InstallationData>> patchedClasses = new LinkedHashMap<>();

        //Recursively check the causes (in case there are multiple exceptions)
        //Need to check for which exceptions were already visited as an exception referring to a prior exception could cause an infinite loop
        Set<Throwable> visited = new HashSet<>();
        Throwable current = throwable;
        while (current != null && !visited.contains(current)) {
            visited.add(current);

            for (StackTraceElement element : current.getStackTrace()) {
                String className = element.getClassName();
                if (patchedClasses.containsKey(className)) continue;

                Set<InstallationData> patches = PatchedClassRegistry.getPatches(className);
                if (!patches.isEmpty()) patchedClasses.put(className, patches);
            }

            current = current.getCause();
        }

        if (patchedClasses.isEmpty()) return null;

        //Every line starts with a line break, which keeps the note off the line with the logs header.
        StringBuilder builder = new StringBuilder();
        appendLine(builder, "Some classes in the stack trace above were patched by mods.");
        appendLine(builder, "The following patches may be involved. Note that they may also be unrelated to the issue.");
        for (Map.Entry<String, Set<InstallationData>> entry : patchedClasses.entrySet()) {
            appendLine(builder, "");
            appendLine(builder, entry.getKey());
            appendPatchTree(builder, entry.getValue());
        }
        return builder.toString();
    }

    /** Groups the patches as mod -> patch class -> handler methods. */
    private void appendPatchTree(StringBuilder builder, Set<InstallationData> patches) {
        Map<String, Map<String, List<String>>> mods = new LinkedHashMap<>();
        for (InstallationData data : patches) {
            mods.computeIfAbsent(data.spec().sourceMod().getName(), key -> new LinkedHashMap<>())
                    .computeIfAbsent(data.spec().handlerClassName(), key -> new ArrayList<>())
                    .add(data.spec().handlerMethodName());
        }

        for (Map.Entry<String, Map<String, List<String>>> mod : mods.entrySet()) {
            String modIndent = appendBranch(builder, "", mod.getKey());

            for (Map.Entry<String, List<String>> handlerClass : mod.getValue().entrySet()) {
                String classIndent = appendBranch(builder, modIndent, handlerClass.getKey());

                for (String handlerMethod : handlerClass.getValue()) {
                    appendBranch(builder, classIndent, handlerMethod);
                }
            }
        }
    }

    /** Appends a tree line and returns the indent for its children. */
    private String appendBranch(StringBuilder builder, String indent, String label) {
        appendLine(builder, indent + "- " + label);
        return indent + "   ";
    }

    private void appendLine(StringBuilder builder, String line) {
        builder.append(Layout.LINE_SEP).append(PREFIX).append(line);
    }

    @Override
    public boolean requiresLayout() {
        return false;
    }

    @Override
    public void close() {

    }

}
