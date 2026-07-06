package patchlib.agent.patch.template;

import net.bytebuddy.asm.Advice;
import net.bytebuddy.implementation.bytecode.assign.Assigner;
import patchlib.agent.dispatch.AfterChainMarker;
import patchlib.agent.dispatch.DispatchIdMarker;
import patchlib.agent.dispatch.PatchDispatcher;
import patchlib.agent.context.HookContextImpl;

import java.lang.invoke.MethodHandle;

/** Exit halves of the constant dispatch templates. Same shapes and pairing as ExitTemplates,
 * but the site's after chain arrives as a dynamic constant instead of a site id.
 *
 * The Except variants dispatch the throw path through the site id like legacy dispatch. Two
 * dynamic constants in one advice method fall off the JIT fast path on the game's Java 17, and
 * with an exception already in flight the registry lookup does not matter. */
public final class ConstantExitTemplates {

    /** Value-returning site with @Except patches. */
    public static final class ValueExcept {
        @Advice.OnMethodExit(onThrowable = Throwable.class)
        public static void exit(
                @AfterChainMarker MethodHandle afterChain,
                @DispatchIdMarker int siteId,
                @Advice.Return(readOnly = false, typing = Assigner.Typing.DYNAMIC) Object returned,
                @Advice.Thrown(readOnly = false, typing = Assigner.Typing.DYNAMIC) Throwable thrown,
                @Advice.Local("context") HookContextImpl context) throws Throwable {

            if (thrown != null) {
                thrown = PatchDispatcher.except(siteId, context, thrown);
                if (thrown == null) {
                    returned = PatchDispatcher.exitConstant(context, context.getReturnValue(), afterChain);
                }
            } else {
                returned = PatchDispatcher.exitConstant(context, returned, afterChain);
            }
        }
    }

    /** Value-returning site without @Except patches. */
    public static final class Value {
        @Advice.OnMethodExit
        public static void exit(
                @AfterChainMarker MethodHandle afterChain,
                @Advice.Return(readOnly = false, typing = Assigner.Typing.DYNAMIC) Object returned,
                @Advice.Local("context") HookContextImpl context) throws Throwable {

            returned = PatchDispatcher.exitConstant(context, returned, afterChain);
        }
    }

    /** Void site with @Except patches. */
    public static final class VoidExcept {
        @Advice.OnMethodExit(onThrowable = Throwable.class)
        public static void exit(
                @AfterChainMarker MethodHandle afterChain,
                @DispatchIdMarker int siteId,
                @Advice.Thrown(readOnly = false, typing = Assigner.Typing.DYNAMIC) Throwable thrown,
                @Advice.Local("context") HookContextImpl context) throws Throwable {

            if (thrown != null) {
                thrown = PatchDispatcher.except(siteId, context, thrown);
                if (thrown == null) {
                    PatchDispatcher.exitConstant(context, null, afterChain);
                }
            } else {
                PatchDispatcher.exitConstant(context, null, afterChain);
            }
        }
    }

    /** Void site without @Except patches. */
    public static final class NoValue {
        @Advice.OnMethodExit
        public static void exit(
                @AfterChainMarker MethodHandle afterChain,
                @Advice.Local("context") HookContextImpl context) throws Throwable {

            PatchDispatcher.exitConstant(context, null, afterChain);
        }
    }

    /** Constructors share one exit: no skip, no exception handling, self only exists now. */
    public static final class Constructor {
        @Advice.OnMethodExit
        public static void exit(
                @AfterChainMarker MethodHandle afterChain,
                @Advice.This(optional = true) Object self,
                @Advice.Enter HookContextImpl context) throws Throwable {

            context.setSelf(self);
            PatchDispatcher.exitConstant(context, null, afterChain);
        }
    }
}
