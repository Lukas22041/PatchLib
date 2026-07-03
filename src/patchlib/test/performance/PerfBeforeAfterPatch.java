package patchlib.test.performance;

import patchlib.api.context.AfterContext;
import patchlib.api.context.BeforeContext;
import patchlib.api.match.ClassMatch;
import patchlib.api.match.MethodMatch;
import patchlib.api.patch.After;
import patchlib.api.patch.Before;
import patchlib.api.patch.Patch;

/** Combined empty @Before and @After, measures the overhead of the combination. */
@Patch(target = @ClassMatch(typeName = "patchlib.test.targets.perf.PerfBeforeAfter"))
public class PerfBeforeAfterPatch {

    @Before(target = @MethodMatch(methodName = "baseline"))
    public static void before(BeforeContext context) {
    }

    @After(target = @MethodMatch(methodName = "baseline"))
    public static void after(AfterContext context) {
    }

}
