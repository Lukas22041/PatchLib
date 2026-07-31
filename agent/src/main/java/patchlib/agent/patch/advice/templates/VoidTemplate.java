package patchlib.agent.patch.advice.templates;

import net.bytebuddy.asm.Advice;
import net.bytebuddy.implementation.bytecode.assign.Assigner;
import patchlib.agent.context.HookContextImpl;
import patchlib.agent.patch.SiteIdMarker;
import patchlib.agent.patch.advice.AdviceDispatcher;
import patchlib.agent.patch.advice.AfterHandleMarker;
import patchlib.agent.patch.advice.BeforeHandleMarker;

import java.lang.invoke.MethodHandle;

public class VoidTemplate {

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

    @Advice.OnMethodExit(onThrowable = Throwable.class)
    public static void exit(
            @SiteIdMarker int siteId,
            @AfterHandleMarker MethodHandle afterHandle,
            @Advice.Thrown(readOnly = false, typing = Assigner.Typing.DYNAMIC) Throwable thrown,
            @Advice.Local("context") HookContextImpl context) {

        if (thrown != null) {
            thrown = AdviceDispatcher.except(siteId, context, thrown);

            //Throw was caught and handled, so any patch is fine to run
            if (thrown == null) {
                AdviceDispatcher.exit(siteId, afterHandle, context, null);
            }
        } else {
           AdviceDispatcher.exit(siteId, afterHandle, context, null);
        }

    }

}
