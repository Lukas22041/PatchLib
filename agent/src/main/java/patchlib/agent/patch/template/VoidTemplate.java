package patchlib.agent.patch.template;

import net.bytebuddy.asm.Advice;
import net.bytebuddy.implementation.bytecode.assign.Assigner;
import patchlib.agent.dispatch.DispatchIdMarker;
import patchlib.agent.dispatch.PatchDispatcher;
import patchlib.agent.context.PatchContext;

/**A template of code that bytebuddy inserts for non-constructor methods that do not have a return type.  */
public final class VoidTemplate {

    @Advice.OnMethodEnter(skipOn = Advice.OnNonDefaultValue.class)
    public static boolean enter(
            @DispatchIdMarker int siteId,
            @Advice.Origin Class<?> owner,
            @Advice.This(optional = true) Object self,
            @Advice.AllArguments(readOnly = false, typing = Assigner.Typing.DYNAMIC) Object[] args,
            @Advice.Local("context") PatchContext context
        ) {

        context = PatchDispatcher.enter(siteId, owner, self, args);

        //Assign the args back in to the method, which applies any changes made to them
        args = context.getArgs();

        return context.isSkipOriginal();
    }

    @Advice.OnMethodExit(onThrowable = Throwable.class)
    public static void exit(
            @DispatchIdMarker int siteId,
            @Advice.Thrown(readOnly = false, typing = Assigner.Typing.DYNAMIC) Throwable thrown,
            @Advice.Local("context") PatchContext context) {

        //Exception
        if (thrown != null) {
            thrown = PatchDispatcher.except(siteId, context, thrown);
            if (thrown == null) {
                PatchDispatcher.exit(siteId, context, null);
            }
        }
        //No Exception
        else {
            PatchDispatcher.exit(siteId, context, null);
        }
    }

}
