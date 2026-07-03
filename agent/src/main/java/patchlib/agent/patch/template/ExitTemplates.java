package patchlib.agent.patch.template;

import net.bytebuddy.asm.Advice;
import net.bytebuddy.implementation.bytecode.assign.Assigner;
import patchlib.agent.dispatch.DispatchIdMarker;
import patchlib.agent.dispatch.PatchDispatcher;
import patchlib.agent.context.PatchContext;

/** Exit halves of the advice templates. The Except variants carry the try/catch wrapper
 * and exception routing; the plain variants leave the host method's exception flow untouched. */
public final class ExitTemplates {

    /** Value-returning site with @Except patches. */
    public static final class ValueExcept {
        @Advice.OnMethodExit(onThrowable = Throwable.class)
        public static void exit(
                @DispatchIdMarker int siteId,
                @Advice.Return(readOnly = false, typing = Assigner.Typing.DYNAMIC) Object returned,
                @Advice.Thrown(readOnly = false, typing = Assigner.Typing.DYNAMIC) Throwable thrown,
                @Advice.Local("context") PatchContext context) {

            if (thrown != null) {
                thrown = PatchDispatcher.except(siteId, context, thrown);
                if (thrown == null) {
                    returned = PatchDispatcher.exit(siteId, context, context.getReturnValue());
                }
            } else {
                returned = PatchDispatcher.exit(siteId, context, returned);
            }
        }
    }

    /** Value-returning site without @Except patches. */
    public static final class Value {
        @Advice.OnMethodExit
        public static void exit(
                @DispatchIdMarker int siteId,
                @Advice.Return(readOnly = false, typing = Assigner.Typing.DYNAMIC) Object returned,
                @Advice.Local("context") PatchContext context) {

            returned = PatchDispatcher.exit(siteId, context, returned);
        }
    }

    /** Void site with @Except patches. */
    public static final class VoidExcept {
        @Advice.OnMethodExit(onThrowable = Throwable.class)
        public static void exit(
                @DispatchIdMarker int siteId,
                @Advice.Thrown(readOnly = false, typing = Assigner.Typing.DYNAMIC) Throwable thrown,
                @Advice.Local("context") PatchContext context) {

            if (thrown != null) {
                thrown = PatchDispatcher.except(siteId, context, thrown);
                if (thrown == null) {
                    PatchDispatcher.exit(siteId, context, null);
                }
            } else {
                PatchDispatcher.exit(siteId, context, null);
            }
        }
    }

    /** Void site without @Except patches. */
    public static final class NoValue {
        @Advice.OnMethodExit
        public static void exit(
                @DispatchIdMarker int siteId,
                @Advice.Local("context") PatchContext context) {

            PatchDispatcher.exit(siteId, context, null);
        }
    }

    /** Constructors share one exit: no skip, no exception handling, self only exists now. */
    public static final class Constructor {
        @Advice.OnMethodExit
        public static void exit(
                @DispatchIdMarker int siteId,
                @Advice.This(optional = true) Object self,
                @Advice.Enter PatchContext context) {

            context.setSelf(self);
            PatchDispatcher.exit(siteId, context, null);
        }
    }
}
