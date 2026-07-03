package patchlib.test.performance;

import patchlib.api.context.FieldReadContext;
import patchlib.api.match.ClassMatch;
import patchlib.api.match.FieldMatch;
import patchlib.api.match.MethodMatch;
import patchlib.api.patch.Patch;
import patchlib.api.patch.RedirectFieldRead;

/** Pass-through @RedirectFieldRead, measures pure interception overhead. */
@Patch(target = @ClassMatch(typeName = "patchlib.test.targets.perf.PerfFieldRead"))
public class PerfFieldReadPatch {

    @RedirectFieldRead(target = @MethodMatch(methodName = "baseline"), field = @FieldMatch(fieldName = "lastValue"))
    public static void passThrough(FieldReadContext context) {
        context.setResult(context.read());
    }

}
