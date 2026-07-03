package patchlib.test.performance;

import patchlib.api.context.AfterContext;
import patchlib.api.match.ClassMatch;
import patchlib.api.match.MethodMatch;
import patchlib.api.patch.After;
import patchlib.api.patch.Patch;

/** Empty @After handler, measures pure interception overhead. */
@Patch(target = @ClassMatch(typeName = "patchlib.test.targets.perf.PerfAfter"))
public class PerfAfterPatch {

    @After(target = @MethodMatch(methodName = "baseline"))
    public static void after(AfterContext context) {
    }

}
