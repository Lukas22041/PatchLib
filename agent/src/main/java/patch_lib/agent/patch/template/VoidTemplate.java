package patch_lib.agent.patch.template;

import net.bytebuddy.asm.Advice;
import net.bytebuddy.implementation.bytecode.assign.Assigner;
import patch_lib.agent.dispatch.DispatchIdMarker;
import patch_lib.agent.dispatch.PatchDispatcher;
import patch_lib.api.PatchContext;

/**A template of code that bytebuddy inserts for non-constructor methods that do not have a return type.  */
public final class VoidTemplate {

    @Advice.OnMethodEnter(skipOn = Advice.OnNonDefaultValue.class)
    public static boolean enter(
            @DispatchIdMarker int siteId,
            @Advice.This(optional = true) Object self,
            @Advice.AllArguments(readOnly = false, typing = Assigner.Typing.DYNAMIC) Object[] args,
            @Advice.Local("context") PatchContext context
        ) {

        context = PatchDispatcher.enter(siteId, self, args);
        return context.isSkipOriginal();
    }

    @Advice.OnMethodExit(/*onThrowable = Throwable.class*/)
    public static void exit(
            @DispatchIdMarker int siteId,
            /*@Advice.Thrown(readOnly = false, typing = Assigner.Typing.DYNAMIC) Throwable thrown,*/
            @Advice.Local("context") PatchContext context) {
        PatchDispatcher.exit(siteId, context, null);
    }

}
