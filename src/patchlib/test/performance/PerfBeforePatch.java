package patchlib.test.performance;

import patchlib.api.context.BeforeContext;
import patchlib.api.match.ClassMatch;
import patchlib.api.match.MethodMatch;
import patchlib.api.patch.Before;
import patchlib.api.patch.Patch;

/** Empty @Before handler, measures pure interception overhead. */
@Patch(target = @ClassMatch(typeName = "patchlib.test.targets.perf.PerfBefore"))
public class PerfBeforePatch {

    @Before(target = @MethodMatch(methodName = "baseline"))
    public static void before(BeforeContext context) {
    }

}
