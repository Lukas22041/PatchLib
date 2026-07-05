package patchlib.test.regression;

import patchlib.api.context.AfterContext;
import patchlib.api.context.BeforeContext;
import patchlib.api.context.ExceptContext;
import patchlib.api.context.FieldReadContext;
import patchlib.api.context.FieldWriteContext;
import patchlib.api.context.ConstructorCallContext;
import patchlib.api.context.MethodCallContext;
import patchlib.api.match.ClassMatch;
import patchlib.api.match.FieldMatch;
import patchlib.api.match.MethodMatch;
import patchlib.api.patch.After;
import patchlib.api.patch.Before;
import patchlib.api.patch.Except;
import patchlib.api.patch.Patch;
import patchlib.api.patch.RedirectCall;
import patchlib.api.patch.RedirectFieldRead;
import patchlib.api.patch.RedirectFieldWrite;
import patchlib.api.patch.RedirectNew;
import patchlib.test.targets.TestBox;

/** One handler per patch annotation, each targeting its own method on RegressionTarget.
 * RegressionTests asserts the effects. */
@Patch(target = @ClassMatch(typeName = "patchlib.test.targets.RegressionTarget"))
public class RegressionPatches {

    @Before(target = @MethodMatch(methodName = "nonExistantMethod"))
    public static void doNotPatch(BeforeContext context) { }

    @Before(target = @MethodMatch(methodName = "beforeTarget"))
    public static void replaceReturn(BeforeContext context) {
        context.skipOriginal("patched");
    }

    @After(target = @MethodMatch(methodName = "afterTarget"))
    public static void adjustReturn(AfterContext context) {
        int value = context.getInferredReturnValue();
        context.setReturnValue(value + 41);
    }

    @Except(target = @MethodMatch(methodName = "exceptTarget"))
    public static void suppress(ExceptContext context) {
        context.suppressException("suppressed");
    }

    /** Guards the no-@Except exit path: an exception must propagate unchanged and skip @After. */
    public static boolean afterThrowRan = false;

    @After(target = @MethodMatch(methodName = "afterThrowTarget"))
    public static void afterOnThrow(AfterContext context) {
        afterThrowRan = true;
    }

    @RedirectCall(target = @MethodMatch(methodName = "redirectCallTarget"), call = @MethodMatch(methodName = "callee"))
    public static void replaceCallArg(MethodCallContext context) {
        context.setCallArg(0, 5);
        context.setResult(context.call());
    }

    @RedirectNew(target = @MethodMatch(methodName = "redirectNewTarget"), type = @ClassMatch(typeName = "patchlib.test.targets.TestBox"))
    public static void replaceInstance(ConstructorCallContext context) {
        context.setResult(new TestBox(99));
    }

    @RedirectFieldRead(target = @MethodMatch(methodName = "redirectReadTarget"), field = @FieldMatch(fieldName = "readField"))
    public static void replaceRead(FieldReadContext context) {
        context.setResult(21);
    }

    @RedirectFieldWrite(target = @MethodMatch(methodName = "redirectWriteTarget"), field = @FieldMatch(fieldName = "writeField"))
    public static void doubleWrite(FieldWriteContext context) {
        int value = context.getInferredValue();
        context.setValue(value * 2);
        context.write();
    }

}
