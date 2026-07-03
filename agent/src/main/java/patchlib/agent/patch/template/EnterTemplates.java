package patchlib.agent.patch.template;

import net.bytebuddy.asm.Advice;
import net.bytebuddy.implementation.bytecode.assign.Assigner;
import patchlib.agent.dispatch.DispatchIdMarker;
import patchlib.agent.dispatch.PatchDispatcher;
import patchlib.agent.context.PatchContext;

/** Enter halves of the advice templates. PatchInstaller pairs one enter with one exit per site,
 * so a site only carries the machinery its patches actually use. */
public final class EnterTemplates {

    /** For sites with @Before patches: runs them, applies argument changes, and can skip the body. */
    public static final class WithBefore {
        @Advice.OnMethodEnter(skipOn = Advice.OnNonDefaultValue.class)
        public static boolean enter(
                @DispatchIdMarker int siteId,
                @Advice.Origin Class<?> owner,
                @Advice.This(optional = true) Object self,
                @Advice.AllArguments(readOnly = false, typing = Assigner.Typing.DYNAMIC) Object[] args,
                @Advice.Local("context") PatchContext context) {

            context = PatchDispatcher.enter(siteId, owner, self, args);

            //Assign the args back in to the method, which applies any changes made to them
            args = context.getArgs();

            return context.isSkipOriginal();
        }
    }

    /** For sites without @Before patches: only creates the context. Nothing can change
     * arguments or skip the body, so the write-back and skip machinery are omitted. */
    public static final class Plain {
        @Advice.OnMethodEnter
        public static void enter(
                @DispatchIdMarker int siteId,
                @Advice.Origin Class<?> owner,
                @Advice.This(optional = true) Object self,
                @Advice.AllArguments(typing = Assigner.Typing.DYNAMIC) Object[] args,
                @Advice.Local("context") PatchContext context) {

            context = PatchDispatcher.enter(siteId, owner, self, args);
        }
    }

    /** Constructor variant of WithBefore. Self is not available before the constructor ran. */
    public static final class ConstructorWithBefore {
        @Advice.OnMethodEnter //Cant skip constructors
        public static PatchContext enter(
                @DispatchIdMarker int siteId,
                @Advice.Origin Class<?> owner,
                @Advice.AllArguments(readOnly = false, typing = Assigner.Typing.DYNAMIC) Object[] args) {

            PatchContext context = PatchDispatcher.enter(siteId, owner, null, args);

            //Assign the args back in to the method, which applies any changes made to them
            args = context.getArgs();

            return context;
        }
    }

    /** Constructor variant of Plain. */
    public static final class ConstructorPlain {
        @Advice.OnMethodEnter
        public static PatchContext enter(
                @DispatchIdMarker int siteId,
                @Advice.Origin Class<?> owner,
                @Advice.AllArguments(typing = Assigner.Typing.DYNAMIC) Object[] args) {

            return PatchDispatcher.enter(siteId, owner, null, args);
        }
    }
}
