package patch_lib.agent.patch.template;

import net.bytebuddy.asm.Advice;
import patch_lib.agent.dispatch.DispatchIdMarker;
import patch_lib.agent.dispatch.PatchDispatcher;
import patch_lib.api.PatchContext;

/** A template of code that bytebuddy inserts for targeted constructors.
 * Constructors can not be skipped and do not have "self" available in "enter", which makes them require their own template. */
public final class ConstructorTemplate {
    @Advice.OnMethodEnter                                   // no skipOn — you can't not-construct
    public static PatchContext enter(@DispatchIdMarker int siteId,
                                     @Advice.AllArguments Object[] args) {
        return PatchDispatcher.enter(siteId, null, args);       // self not available yet
    }

    @Advice.OnMethodExit
    public static void exit(@DispatchIdMarker int siteId,
                            @Advice.This Object self,
                            @Advice.Enter PatchContext context) {
        context.setSelf(self);
        PatchDispatcher.exit(siteId, context, null);
    }
}
