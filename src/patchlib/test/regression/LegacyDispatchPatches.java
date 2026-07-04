package patchlib.test.regression;

import patchlib.api.context.AfterContext;
import patchlib.api.context.BeforeContext;
import patchlib.api.match.ClassMatch;
import patchlib.api.match.MethodMatch;
import patchlib.api.patch.After;
import patchlib.api.patch.Before;
import patchlib.api.patch.Patch;

/** Handlers for the janino compiled legacy target, see LegacyDispatchTests.
 * Script classes are class file 45 and always use legacy dispatch, this keeps that path covered. */
@Patch(target = @ClassMatch(typeName = "PatchLibLegacyTarget"))
public class LegacyDispatchPatches {

    @Before(target = @MethodMatch(methodName = "beforeTarget"))
    public static void replaceReturn(BeforeContext context) {
        context.skipOriginal("patched");
    }

    @After(target = @MethodMatch(methodName = "afterTarget"))
    public static void adjustReturn(AfterContext context) {
        int value = context.getInferredReturnValue();
        context.setReturnValue(value + 41);
    }

}
