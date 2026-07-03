package patchlib.test.performance;

import patchlib.api.context.AfterContext;
import patchlib.api.context.BeforeContext;
import patchlib.api.context.ExceptContext;
import patchlib.api.match.ClassMatch;
import patchlib.api.match.MethodMatch;
import patchlib.api.patch.After;
import patchlib.api.patch.Before;
import patchlib.api.patch.Except;
import patchlib.api.patch.Patch;

/** Combined empty @Before, @After and @Except, measures the overhead of the combination. */
@Patch(target = @ClassMatch(typeName = "patchlib.test.targets.perf.PerfBeforeAfterExcept"))
public class PerfBeforeAfterExceptPatch {

    @Before(target = @MethodMatch(methodName = "baseline"))
    public static void before(BeforeContext context) {
    }

    @After(target = @MethodMatch(methodName = "baseline"))
    public static void after(AfterContext context) {
    }

    @Except(target = @MethodMatch(methodName = "baseline"))
    public static void except(ExceptContext context) {
    }

}
