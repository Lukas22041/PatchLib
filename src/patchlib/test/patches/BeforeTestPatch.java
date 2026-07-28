package patchlib.test.patches;

import patchlib.api.context.BeforeContext;
import patchlib.api.match.ClassMatch;
import patchlib.api.match.MethodMatch;
import patchlib.api.patch.Before;
import patchlib.api.patch.Patch;
import patchlib.test.targets.BeforeTestTarget;

@Patch(target = @ClassMatch(type = BeforeTestTarget.class))
public class BeforeTestPatch {

    @Before(target = @MethodMatch(methodName = "testReplaceArgTarget"))
    public static void testReplaceArgPatch(BeforeContext context) {
        context.setArg(0, context.getArg(0) + "_REPLACED");
    }

    @Before(target = @MethodMatch(methodName = "testSkipMethodTarget"))
    public static void testSkipMethodPatch(BeforeContext context) {
        context.skipOriginal(context.getArg(0));
    }

}
