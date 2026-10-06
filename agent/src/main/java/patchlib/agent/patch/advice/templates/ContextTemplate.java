package patchlib.agent.patch.advice.templates;

import net.bytebuddy.asm.Advice;
import net.bytebuddy.implementation.bytecode.assign.Assigner;
import patchlib.agent.context.HookContextImpl;
import patchlib.agent.patch.SiteIdMarker;

/** Only creates the context for the exit templates, for sites without @Before patches. */
public final class ContextTemplate {

    @Advice.OnMethodEnter
    public static void enter(
            @SiteIdMarker int siteId,
            @Advice.Origin Class<?> owner,
            @Advice.This(optional = true) Object self,
            @Advice.AllArguments(typing = Assigner.Typing.DYNAMIC) Object[] args,
            @Advice.Local("context") HookContextImpl context) {

        context = new HookContextImpl(owner, self, args, siteId);
    }

}
