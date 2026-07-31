package patchlib.test.patches;

import patchlib.api.context.AfterContext;
import patchlib.api.match.ClassMatch;
import patchlib.api.match.MethodMatch;
import patchlib.api.patch.After;
import patchlib.api.patch.Patch;
import patchlib.api.query.FieldQuery;
import patchlib.api.query.MethodQuery;
import patchlib.api.spec.FieldQuerySpec;
import patchlib.api.spec.MethodQuerySpec;
import patchlib.test.targets.ExceptTestTarget;
import patchlib.test.targets.ReflectionTestTarget;

@Patch(target = @ClassMatch(type = ReflectionTestTarget.class))
public class ReflectionTestPatch {

    @After(target = @MethodMatch(methodName = "testTarget"))
    public static void testTargetPatch(AfterContext context) {
        MethodQuerySpec methodQuerySpec = MethodQuery.create().methodName("testMethod").build();
        FieldQuerySpec fieldQuerySpec = FieldQuery.create().fieldName("testField").build();

        try {
            String methodResult = context.<String>getMethod(methodQuerySpec).call();
            String fieldResult = context.<String>getField(fieldQuerySpec).get();
            context.setReturnValue(fieldResult+methodResult);
        } catch (Throwable ex) {
            context.setReturnValue("EXCEPTION: " + ex.getMessage());
        }
    }

}
