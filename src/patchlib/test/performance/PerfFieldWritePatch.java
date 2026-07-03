package patchlib.test.performance;

import patchlib.api.context.FieldWriteContext;
import patchlib.api.match.ClassMatch;
import patchlib.api.match.FieldMatch;
import patchlib.api.match.MethodMatch;
import patchlib.api.patch.Patch;
import patchlib.api.patch.RedirectFieldWrite;

/** Pass-through @RedirectFieldWrite, measures pure interception overhead. */
@Patch(target = @ClassMatch(typeName = "patchlib.test.targets.perf.PerfFieldWrite"))
public class PerfFieldWritePatch {

    @RedirectFieldWrite(target = @MethodMatch(methodName = "baseline"), field = @FieldMatch(fieldName = "lastValue"))
    public static void passThrough(FieldWriteContext context) {
        context.write();
    }

}
