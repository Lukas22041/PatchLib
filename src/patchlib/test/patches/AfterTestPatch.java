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

    @After(target = @MethodMatch(methodName = "testVoidTarget"))
    public static void testVoidPatch(AfterContext context) {
        AfterTestTarget target = context.getInferredSelf();
        target.afterVoidRan = true;
    }

    @After(target = @MethodMatch(methodName = "testPrimitiveReturnTarget"))
    public static void testPrimitiveReturnPatch(AfterContext context) {
        int returned = context.getInferredReturnValue();
        context.setReturnValue(returned * 2);
    }

    @Before(target = @MethodMatch(methodName = "testBeforeAndAfterTarget"))
    public static void testBeforeAndAfterBeforePatch(BeforeContext context) {
        context.setArg(0, context.getArg(0) + "_BEFORE");
    }

    @After(target = @MethodMatch(methodName = "testBeforeAndAfterTarget"))
    public static void testBeforeAndAfterAfterPatch(AfterContext context) {
        context.setReturnValue(context.getReturnValue() + "_AFTER");
    }

    @After(target = @MethodMatch(methodName = "testStaticTarget"))
    public static void testStaticPatch(AfterContext context) {
        context.setReturnValue(context.getReturnValue() + "_REPLACED");
    }

}
