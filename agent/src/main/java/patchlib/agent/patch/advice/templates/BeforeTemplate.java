package patchlib.agent.patch.advice.templates;

import net.bytebuddy.asm.Advice;
import net.bytebuddy.implementation.bytecode.assign.Assigner;
import patchlib.agent.context.HookContextImpl;
import patchlib.agent.patch.SiteIdMarker;
import patchlib.agent.patch.advice.AdviceDispatcher;
import patchlib.agent.patch.advice.BeforeHandleMarker;

import java.lang.invoke.MethodHandle;

public final class BeforeTemplate {

    @Advice.OnMethodEnter(skipOn = Advice.OnNonDefaultValue.class)
    public static boolean enter(
            @SiteIdMarker int siteId,
            @BeforeHandleMarker MethodHandle beforeHandle,
            @Advice.Origin Class<?> owner,
            @Advice.This(optional = true) Object self,
            @Advice.AllArguments(readOnly = false, typing = Assigner.Typing.DYNAMIC) Object[] args,
            @Advice.Local("context") HookContextImpl context) {

        context = AdviceDispatcher.enter(siteId, beforeHandle, owner, self, args);

        //Re-assign the args to apply any changes
        args = context.getArgs();

        return context.isSkipOriginal();
    }

}
