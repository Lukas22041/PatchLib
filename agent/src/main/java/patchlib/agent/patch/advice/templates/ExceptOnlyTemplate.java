package patchlib.agent.patch.advice.templates;

import net.bytebuddy.asm.Advice;
import net.bytebuddy.implementation.bytecode.assign.Assigner;
import patchlib.agent.context.HookContextImpl;
import patchlib.agent.patch.SiteIdMarker;
import patchlib.agent.patch.advice.AdviceDispatcher;

/**
 * Exit for sites with only @Except patches. Has no enter template, the context is only created once something is thrown,
 * so the method has no overhead otherwise. The args are the original ones, as Byte Buddy backs them up for the exit.
 */
public final class ExceptOnlyTemplate {

    @Advice.OnMethodExit(onThrowable = Throwable.class)
    public static void exit(
            @SiteIdMarker int siteId,
            @Advice.Origin Class<?> owner,
            @Advice.This(optional = true) Object self,
            @Advice.AllArguments(typing = Assigner.Typing.DYNAMIC) Object[] args,
            @Advice.Return(readOnly = false, typing = Assigner.Typing.DYNAMIC) Object returned,
            @Advice.Thrown(readOnly = false, typing = Assigner.Typing.DYNAMIC) Throwable thrown) {

        if (thrown != null) {
            HookContextImpl context = new HookContextImpl(owner, self, args, siteId);
            thrown = AdviceDispatcher.except(siteId, context, thrown);

            if (thrown == null) {
                returned = context.getReturnValue();
            }
        }
    }

}
