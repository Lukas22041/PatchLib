package patchlib.test.performance;

import patchlib.api.context.MethodCallContext;
import patchlib.api.match.ClassMatch;
import patchlib.api.match.MethodMatch;
import patchlib.api.patch.Patch;
import patchlib.api.patch.RedirectCall;

/** Pass-through @RedirectCall, measures pure interception overhead. */
@Patch(target = @ClassMatch(typeName = "patchlib.test.targets.perf.PerfRedirectCall"))
public class PerfRedirectCallPatch {

    @RedirectCall(target = @MethodMatch(methodName = "baseline"), call = @MethodMatch(methodName = "helperInstance"))
    public static void passThrough(MethodCallContext context) {
        context.setResult(context.call());
    }

}
