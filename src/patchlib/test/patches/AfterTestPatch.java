package patchlib.test.patches;

import patchlib.api.context.AfterContext;
import patchlib.api.context.BeforeContext;
import patchlib.api.match.ClassMatch;
import patchlib.api.match.MethodMatch;
import patchlib.api.patch.After;
import patchlib.api.patch.Before;
import patchlib.api.patch.Patch;
import patchlib.test.targets.AfterTestTarget;

@Patch(target = @ClassMatch(type = AfterTestTarget.class))
public class AfterTestPatch {

    @After(target = @MethodMatch(methodName = "testReplaceReturnValueTarget"))
    public static void testReplaceReturnValuePatch(AfterContext context) {
        context.setReturnValue(context.getReturnValue() + "_REPLACED");
    }

}
