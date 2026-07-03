package patchlib.test.performance;

import patchlib.api.context.ConstructorCallContext;
import patchlib.api.match.ClassMatch;
import patchlib.api.match.MethodMatch;
import patchlib.api.patch.Patch;
import patchlib.api.patch.RedirectNew;

/** Pass-through @RedirectNew, measures pure interception overhead. */
@Patch(target = @ClassMatch(typeName = "patchlib.test.targets.perf.PerfRedirectNew"))
public class PerfRedirectNewPatch {

    @RedirectNew(target = @MethodMatch(methodName = "baseline"), type = @ClassMatch(typeName = "patchlib.test.targets.TestBox"))
    public static void passThrough(ConstructorCallContext context) {
        context.setResult(context.call());
    }

}
