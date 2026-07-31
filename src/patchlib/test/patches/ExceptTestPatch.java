package patchlib.test.patches;

import patchlib.api.context.AfterContext;
import patchlib.api.context.ExceptContext;
import patchlib.api.match.ClassMatch;
import patchlib.api.match.MethodMatch;
import patchlib.api.patch.After;
import patchlib.api.patch.Except;
import patchlib.api.patch.Patch;
import patchlib.test.targets.AfterTestTarget;
import patchlib.test.targets.ExceptTestTarget;

@Patch(target = @ClassMatch(type = ExceptTestTarget.class))
public class ExceptTestPatch {

    @Except(target = @MethodMatch(methodName = "testSuppressExceptionTarget"))
    public static void testReplaceReturnValuePatch(ExceptContext context) {
        context.suppressException(context.getArg(0) + "_SUPPRESSED");
    }

    @Except(target = @MethodMatch(methodName = "testReplaceExceptionTarget"))
    public static void testReplaceExceptionTargetPatch(ExceptContext context) {
        context.replaceThrown(new RuntimeException(context.getThrown().getMessage()+"_REPLACED"));
    }

}
