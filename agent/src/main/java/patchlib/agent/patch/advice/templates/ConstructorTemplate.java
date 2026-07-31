package patchlib.agent.patch.advice.templates;

import net.bytebuddy.asm.Advice;
import net.bytebuddy.implementation.bytecode.assign.Assigner;
import patchlib.agent.context.HookContextImpl;
import patchlib.agent.patch.SiteIdMarker;
import patchlib.agent.patch.advice.AdviceDispatcher;
import patchlib.agent.patch.advice.AfterHandleMarker;
import patchlib.agent.patch.advice.BeforeHandleMarker;

import java.lang.invoke.MethodHandle;

public class ConstructorTemplate {


    @Advice.OnMethodEnter()
    public static void enter(
            @SiteIdMarker int siteId,
            @BeforeHandleMarker MethodHandle beforeHandle,
            @Advice.Origin Class<?> owner,
            @Advice.AllArguments(readOnly = false, typing = Assigner.Typing.DYNAMIC) Object[] args,
            @Advice.Local("context") HookContextImpl context) {

        context = AdviceDispatcher.enter(siteId, beforeHandle, owner, null, args);

        //Re-assign the args to apply any changes
        args = context.getArgs();
    }


    @Advice.OnMethodExit()
    public static void exit(
            @SiteIdMarker int siteId,
            @AfterHandleMarker MethodHandle afterHandle,
            @Advice.This(optional = true) Object self,
            @Advice.Local("context") HookContextImpl context) {

        context.setSelf(self);
        AdviceDispatcher.exit(siteId, afterHandle, context, null);
    }

}
