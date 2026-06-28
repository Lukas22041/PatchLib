package patchlib.agent.patch.template;

import net.bytebuddy.asm.Advice;
import net.bytebuddy.implementation.bytecode.assign.Assigner;
import patchlib.agent.dispatch.DispatchIdMarker;
import patchlib.agent.dispatch.PatchDispatcher;
import patchlib.api.PatchContext;

/** A template of code that bytebuddy inserts for targeted constructors.
 * Constructors can not be skipped and do not have "self" available in "enter", which makes them require their own template. */
public final class ConstructorTemplate {
    @Advice.OnMethodEnter //Cant skip constructors
    public static PatchContext enter(@DispatchIdMarker int siteId,
                                     @Advice.Origin Class<?> owner,
                                     @Advice.AllArguments(readOnly = false, typing = Assigner.Typing.DYNAMIC) Object[] args) {
        PatchContext context = PatchDispatcher.enter(siteId, owner, null, args); //Self not available before the constructor ran.

        //Assign the args back in to the method, which applies any changes made to them
        args = context.getArgs();

        return context;
    }

    @Advice.OnMethodExit(onThrowable = Throwable.class)
    public static void exit(@DispatchIdMarker int siteId,
                            @Advice.This(optional = true) Object self,
                            @Advice.Thrown(readOnly = false, typing = Assigner.Typing.DYNAMIC) Throwable thrown,
                            @Advice.Enter PatchContext context) {

        //Exception
        if (thrown != null) {
            context.setAllowSuppress(false);
            thrown = PatchDispatcher.except(siteId, context, thrown);
        } else {
            //Normal completion.
            context.setSelf(self);
            PatchDispatcher.exit(siteId, context, null);
        }
    }
}
