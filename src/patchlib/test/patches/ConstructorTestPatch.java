package patchlib.test.patches;

import patchlib.api.context.AfterContext;
import patchlib.api.context.BeforeContext;
import patchlib.api.match.ClassMatch;
import patchlib.api.match.MethodMatch;
import patchlib.api.match.MethodType;
import patchlib.api.patch.After;
import patchlib.api.patch.Before;
import patchlib.api.patch.Patch;
import patchlib.test.targets.ConstructorTestTarget;

@Patch(target = @ClassMatch(type = ConstructorTestTarget.class))
public class ConstructorTestPatch {

    @Before(target = @MethodMatch(methodType = MethodType.CONSTRUCTOR))
    public static void testConstructorBeforePatch(BeforeContext context) {
        context.setArg(0, context.getArg(0) + "_BEFORE");
    }

    @After(target = @MethodMatch(methodType = MethodType.CONSTRUCTOR))
    public static void testConstructorAfterPatch(AfterContext context) {
        ConstructorTestTarget target = context.getInferredSelf();
        target.value = target.value + "_AFTER";
    }

}
