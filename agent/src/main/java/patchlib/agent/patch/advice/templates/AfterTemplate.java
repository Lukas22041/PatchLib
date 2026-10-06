package patchlib.agent.patch.advice.templates;

import net.bytebuddy.asm.Advice;
import net.bytebuddy.implementation.bytecode.assign.Assigner;
import patchlib.agent.context.HookContextImpl;
import patchlib.agent.patch.SiteIdMarker;
import patchlib.agent.patch.advice.AdviceDispatcher;
import patchlib.agent.patch.advice.AfterHandleMarker;

import java.lang.invoke.MethodHandle;

public final class AfterTemplate {

    @Advice.OnMethodExit
    public static void exit(
            @SiteIdMarker int siteId,
            @AfterHandleMarker MethodHandle afterHandle,
            @Advice.Return(readOnly = false, typing = Assigner.Typing.DYNAMIC) Object returned,
            @Advice.Local("context") HookContextImpl context) {

        returned = AdviceDispatcher.exit(siteId, afterHandle, context, returned);
    }

}
