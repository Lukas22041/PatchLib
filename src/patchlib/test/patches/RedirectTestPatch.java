package patchlib.test.patches;

import patchlib.api.context.ConstructorCallContext;
import patchlib.api.context.FieldReadContext;
import patchlib.api.context.FieldWriteContext;
import patchlib.api.context.MethodCallContext;
import patchlib.api.match.ClassMatch;
import patchlib.api.match.FieldMatch;
import patchlib.api.match.MethodMatch;
import patchlib.api.patch.*;
import patchlib.test.targets.RedirectTestTarget;

@Patch(target = @ClassMatch(type = RedirectTestTarget.class))
public class RedirectTestPatch {

    @RedirectMethodCall(target = @MethodMatch(methodName = "testMethodCallRedirect"), call = @MethodMatch(methodName = "testCall"))
    public static void testMethodCallRedirectPatch(MethodCallContext context) {
        String value = (String) context.call("TEST_REPLACED");
        context.setResult(value);
    }

    @RedirectConstructorCall(target = @MethodMatch(methodName = "testConstructorCallRedirect"), type = @ClassMatch(type = RedirectTestTarget.RedirectConstructorTest.class))
    public static void testConstructorCallRedirectPatch(ConstructorCallContext context) {
        RedirectTestTarget.RedirectConstructorTest value = (RedirectTestTarget.RedirectConstructorTest) context.call("TEST_REPLACED");
        context.setResult(value);
    }

    @RedirectFieldRead(target = @MethodMatch(methodName = "testFieldReadRedirect"), field = @FieldMatch(fieldName = "testField"))
    public static void testFieldReadRedirectPatch(FieldReadContext context) {
        String value = (String) context.read();
        context.setResult(value + "_REPLACED");
    }

    @RedirectFieldWrite(target = @MethodMatch(methodName = "testFieldWriteRedirect"), field = @FieldMatch(fieldName = "testField"))
    public static void testFieldWriteRedirectPatch(FieldWriteContext context) {
        String written = context.getInferredValueToWrite();
        context.write(written + "_REPLACED");
    }


    @RedirectMethodCall(target = @MethodMatch(methodName = "testRedirectLayers"), call = @MethodMatch(methodName = "testCall"), order = 5)
    public static void testRedirectLayer3Patch(MethodCallContext context) {
        String value = (String) context.call();
        context.setResult(value + "3");
    }

    @RedirectMethodCall(target = @MethodMatch(methodName = "testRedirectLayers"), call = @MethodMatch(methodName = "testCall"), order = 10)
    public static void testRedirectLayer2Patch(MethodCallContext context) {
        String value = (String) context.call();
        context.setResult(value + "2");
    }
}
