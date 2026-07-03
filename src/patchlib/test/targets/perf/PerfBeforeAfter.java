package patchlib.test.targets.perf;

import patchlib.test.targets.TestBox;

/** Perf target patched with @Before and @After combined. */
public class PerfBeforeAfter {

    public long lastValue;

    /** Identical in every perf target class. Mixes math, string building, an instance call,
     * a static call, a construction, a field write and a field read, so every patch type has a hook. */
    public long baseline(int seed) {
        int acc = seed;
        for (int i = 0; i < 16; i++) acc = acc * 31 + i; //math
        StringBuilder sb = new StringBuilder(16); //string building
        sb.append("v").append(acc & 0xFF);
        acc += helperInstance(acc); //@RedirectCall site
        acc += helperStatic(acc); //static call
        TestBox box = new TestBox(acc); //@RedirectNew site
        lastValue = acc; //@RedirectFieldWrite site
        long read = lastValue; //@RedirectFieldRead site
        return read + box.value + sb.length(); //everything feeds the return
    }

    public int helperInstance(int x) {
        return (x ^ (x >>> 7)) & 0xFFFF;
    }

    public static int helperStatic(int x) {
        return (x * 17) & 0xFFFF;
    }

}
