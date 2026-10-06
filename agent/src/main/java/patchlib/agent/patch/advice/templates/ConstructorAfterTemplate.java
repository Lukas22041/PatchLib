package patchlib.agent.patch.advice.templates;

import net.bytebuddy.asm.Advice;
import patchlib.agent.context.HookContextImpl;
import patchlib.agent.patch.SiteIdMarker;
import patchlib.agent.patch.advice.AdviceDispatcher;
import patchlib.agent.patch.advice.AfterHandleMarker;

import java.lang.invoke.MethodHandle;

public final class ConstructorAfterTemplate {

    @Advice.OnMethodExit
    public static void exit(
            @SiteIdMarker int siteId,
            @AfterHandleMarker MethodHandle afterHandle,
            @Advice.This Object self,
            @Advice.Local("context") HookContextImpl context) {

        context.setSelf(self);
        AdviceDispatcher.exit(siteId, afterHandle, context, null);
    }

}
