package patchlib.agent.patch.template;

import net.bytebuddy.asm.Advice;
import net.bytebuddy.implementation.bytecode.assign.Assigner;
import patchlib.agent.dispatch.DispatchIdMarker;
import patchlib.agent.dispatch.PatchDispatcher;
import patchlib.api.PatchContext;

/** A template of code that bytebuddy inserts for targeted constructors.
 * Constructors are special in three ways, so they need their own template:
 *  - they can not be skipped,
 *  - "self" is not available in "enter" (the instance does not exist yet),
 *  - they can not catch their own thrown exceptions. */
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

    @Advice.OnMethodExit
    public static void exit(@DispatchIdMarker int siteId,
                            @Advice.This(optional = true) Object self,
                            @Advice.Enter PatchContext context) {
        context.setSelf(self);
        PatchDispatcher.exit(siteId, context, null);
    }
}
