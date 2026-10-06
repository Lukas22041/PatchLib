package patchlib.agent.patch.advice.templates;

import net.bytebuddy.asm.Advice;
import net.bytebuddy.implementation.bytecode.assign.Assigner;
import patchlib.agent.context.HookContextImpl;
import patchlib.agent.patch.SiteIdMarker;
import patchlib.agent.patch.advice.AdviceDispatcher;
import patchlib.agent.patch.advice.AfterHandleMarker;

import java.lang.invoke.MethodHandle;

public final class ExceptTemplate {

    @Advice.OnMethodExit(onThrowable = Throwable.class)
    public static void exit(
            @SiteIdMarker int siteId,
            @AfterHandleMarker MethodHandle afterHandle,
            @Advice.Return(readOnly = false, typing = Assigner.Typing.DYNAMIC) Object returned,
            @Advice.Thrown(readOnly = false, typing = Assigner.Typing.DYNAMIC) Throwable thrown,
            @Advice.Local("context") HookContextImpl context) {

        if (thrown != null) {
            thrown = AdviceDispatcher.except(siteId, context, thrown);

            //Throw was caught and handled, so any patch is fine to run
            if (thrown == null) {
                returned = AdviceDispatcher.exit(siteId, afterHandle, context, context.getReturnValue());
            }
        } else {
            returned = AdviceDispatcher.exit(siteId, afterHandle, context, returned);
        }
    }

}
