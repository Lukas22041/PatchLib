package patchlib.test.performance;

import patchlib.agent.PatchLibLogger;
import patchlib.test.targets.perf.PerfAfter;
import patchlib.test.targets.perf.PerfBaseline;
import patchlib.test.targets.perf.PerfBefore;
import patchlib.test.targets.perf.PerfBeforeAfter;
import patchlib.test.targets.perf.PerfBeforeAfterExcept;
import patchlib.test.targets.perf.PerfExcept;
import patchlib.test.targets.perf.PerfFieldRead;
import patchlib.test.targets.perf.PerfFieldWrite;
import patchlib.test.targets.perf.PerfRedirectCall;
import patchlib.test.targets.perf.PerfRedirectNew;

import java.util.List;
import java.util.function.IntToLongFunction;

/** Times the identical baseline method across differently patched targets, comparing each patch
 * type against the unpatched reference. Indicative numbers, only comparable within one launch. */
public class PerformanceTests {

    private static final int WARMUP = 50000;
    private static final int ROUNDS = 10;
    private static final int CALLS_PER_ROUND = 10000;

    /** Consumed results land here so the JIT can not eliminate the work. */
    public static volatile long sink;

    private record Case(String name, IntToLongFunction body) {}

    public static void run() {
        //First use loads and transforms each target class, outside the timed window
        PerfBaseline baseline = new PerfBaseline();
        PerfBefore before = new PerfBefore();
        PerfAfter after = new PerfAfter();
        PerfExcept except = new PerfExcept();
        PerfRedirectCall redirectCall = new PerfRedirectCall();
        PerfRedirectNew redirectNew = new PerfRedirectNew();
        PerfFieldRead fieldRead = new PerfFieldRead();
        PerfFieldWrite fieldWrite = new PerfFieldWrite();
        PerfBeforeAfter beforeAfter = new PerfBeforeAfter();
        PerfBeforeAfterExcept beforeAfterExcept = new PerfBeforeAfterExcept();

        List<Case> cases = List.of(
                new Case("baseline (unpatched)", seed -> baseline.baseline(seed)),
                new Case("@Before", seed -> before.baseline(seed)),
                new Case("@After", seed -> after.baseline(seed)),
                new Case("@Except", seed -> except.baseline(seed)),
                new Case("@RedirectCall", seed -> redirectCall.baseline(seed)),
                new Case("@RedirectNew", seed -> redirectNew.baseline(seed)),
                new Case("@RedirectFieldRead", seed -> fieldRead.baseline(seed)),
                new Case("@RedirectFieldWrite", seed -> fieldWrite.baseline(seed)),
                new Case("@Before + @After", seed -> beforeAfter.baseline(seed)),
                new Case("@Before + @After + @Except", seed -> beforeAfterExcept.baseline(seed)));

        //The first pass only warms up. Compilation is asynchronous, so cases measured while the
        //compiler is still catching up are not comparable; the second pass runs every case,
        //including the baseline, on equally compiled footing.
        for (Case c : cases) measure(c.name(), c.body(), false);
        for (Case c : cases) measure(c.name(), c.body(), true);
    }

    /** Warmup lets the JIT compile before timing. The varying seed prevents constant folding.
     * The best round filters out rounds disturbed by mid-measurement compilation or GC. */
    private static void measure(String name, IntToLongFunction body, boolean log) {
        for (int i = 0; i < WARMUP; i++) sink += body.applyAsLong(i);
        long best = Long.MAX_VALUE;
        long total = 0;
        for (int round = 0; round < ROUNDS; round++) {
            long start = System.nanoTime();
            for (int i = 0; i < CALLS_PER_ROUND; i++) sink += body.applyAsLong(i);
            long elapsed = System.nanoTime() - start;
            total += elapsed;
            if (elapsed < best) best = elapsed;
        }
        if (log) PatchLibLogger.info(String.format("PERF %-28s best %,6d ns/call, avg %,6d ns/call (%dx%d calls)",
                name + ":", best / CALLS_PER_ROUND, total / (ROUNDS * CALLS_PER_ROUND), ROUNDS, CALLS_PER_ROUND));
    }

}
