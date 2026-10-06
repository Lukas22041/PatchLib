package patchlib.agent.patch.advice.templates;

import net.bytebuddy.asm.Advice;
import net.bytebuddy.implementation.bytecode.assign.Assigner;
import patchlib.agent.context.HookContextImpl;

/** Exit for sites with only @Before patches, to return the value of a skipped method. */
public final class SkipTemplate {

    @Advice.OnMethodExit
    public static void exit(
            @Advice.Return(readOnly = false, typing = Assigner.Typing.DYNAMIC) Object returned,
            @Advice.Local("context") HookContextImpl context) {

        if (context.isSkipOriginal()) {
            returned = context.getReturnValue();
        }
    }

}
