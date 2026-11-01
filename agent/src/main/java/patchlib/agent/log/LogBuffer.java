package patchlib.agent.log;

import org.apache.log4j.AppenderSkeleton;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.spi.LoggingEvent;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/** Keeps the most recent log events in memory for the debug panel. */
public class LogBuffer extends AppenderSkeleton {

    public static final int MAX_ENTRIES = 1000;
    public static final Channel GAME = new Channel();
    /** Kept apart from the game channel, so that other logs can not push PatchLibs entries out. */
    public static final Channel PATCHLIB = new Channel();

    private static final int MAX_TEXT_LENGTH = 4000;
    private static final String PATCHLIB_LOGGER_PREFIX = "patchlib.";

    public record Entry(long index, Level level, String loggerName, String text) { }

    public static class Channel {

        private final ArrayDeque<Entry> entries = new ArrayDeque<>();
        private long nextIndex = 1;

        private synchronized void add(Level level, String loggerName, String text) {
            entries.addLast(new Entry(nextIndex++, level, loggerName, text));
            if (entries.size() > MAX_ENTRIES) entries.removeFirst();
        }

        /** Returns the entries newer than the given index, oldest first. */
        public synchronized List<Entry> getEntriesAfter(long index) {
            List<Entry> newer = new ArrayList<>();
            Iterator<Entry> iterator = entries.descendingIterator();
            while (iterator.hasNext()) {
                Entry entry = iterator.next();
                if (entry.index() <= index) break;
                newer.add(entry);
            }
            Collections.reverse(newer);
            return newer;
        }
    }

    /** Has to be added before PatchTraceAppender. That one logs its note while handling the stack trace, so anything after it would receive the note first. */
    public static void install() {
        Logger.getRootLogger().addAppender(new LogBuffer());
    }

    @Override
    protected void append(LoggingEvent event) {
        String text = createText(event);
        Level level = event.getLevel();

        GAME.add(level, event.getLoggerName(), text);
        if (event.getLoggerName().startsWith(PATCHLIB_LOGGER_PREFIX)) {
            PATCHLIB.add(level, event.getLoggerName(), text);
        }
    }

    private String createText(LoggingEvent event) {
        StringBuilder builder = new StringBuilder(String.valueOf(event.getRenderedMessage()));

        String[] stackTrace = event.getThrowableStrRep();
        if (stackTrace != null) {
            for (String line : stackTrace) {
                builder.append('\n').append(line);
            }
        }

        String text = builder.toString().replace("\r", "").replace("\t", "    ").strip();
        return text.length() > MAX_TEXT_LENGTH ? text.substring(0, MAX_TEXT_LENGTH) + "..." : text;
    }

    @Override
    public boolean requiresLayout() {
        return false;
    }

    @Override
    public void close() {

    }

}
