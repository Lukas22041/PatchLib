package patchlib.test.performance;

import patchlib.api.context.ExceptContext;
import patchlib.api.match.ClassMatch;
import patchlib.api.match.MethodMatch;
import patchlib.api.patch.Except;
import patchlib.api.patch.Patch;

/** Empty @Except handler. The baseline never throws, so this measures the installed
 * exception wrapping on the happy path. */
@Patch(target = @ClassMatch(typeName = "patchlib.test.targets.perf.PerfExcept"))
public class PerfExceptPatch {

    @Except(target = @MethodMatch(methodName = "baseline"))
    public static void except(ExceptContext context) {
    }

}
