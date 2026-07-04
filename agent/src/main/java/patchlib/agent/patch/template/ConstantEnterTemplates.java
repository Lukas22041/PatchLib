package patchlib.agent.patch.template;

import net.bytebuddy.asm.Advice;
import net.bytebuddy.implementation.bytecode.assign.Assigner;
import patchlib.agent.dispatch.BeforeChainMarker;
import patchlib.agent.dispatch.PatchDispatcher;
import patchlib.agent.context.AdviceContextImpl;

import java.lang.invoke.MethodHandle;

/** Enter halves of the constant dispatch templates. Same shapes and pairing as EnterTemplates,
 * but the site's before chain arrives as a dynamic constant instead of a site id, which lets the
 * JIT inline the handlers, see ChainBootstrap. Used for hosts with class file version 55 or newer. */
public final class ConstantEnterTemplates {

    /** For sites with @Before patches: runs them, applies argument changes, and can skip the body. */
    public static final class WithBefore {
        @Advice.OnMethodEnter(skipOn = Advice.OnNonDefaultValue.class)
        public static boolean enter(
                @BeforeChainMarker MethodHandle beforeChain,
                @Advice.Origin Class<?> owner,
                @Advice.This(optional = true) Object self,
                @Advice.AllArguments(readOnly = false, typing = Assigner.Typing.DYNAMIC) Object[] args,
                @Advice.Local("context") AdviceContextImpl context) throws Throwable {

            context = PatchDispatcher.enterConstant(owner, self, args, beforeChain);

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
                @Advice.Origin Class<?> owner,
                @Advice.This(optional = true) Object self,
                @Advice.AllArguments(typing = Assigner.Typing.DYNAMIC) Object[] args,
                @Advice.Local("context") AdviceContextImpl context) {

            context = PatchDispatcher.createContext(owner, self, args);
        }
    }

    /** Constructor variant of WithBefore. Self is not available before the constructor ran. */
    public static final class ConstructorWithBefore {
        @Advice.OnMethodEnter //Cant skip constructors
        public static AdviceContextImpl enter(
                @BeforeChainMarker MethodHandle beforeChain,
                @Advice.Origin Class<?> owner,
                @Advice.AllArguments(readOnly = false, typing = Assigner.Typing.DYNAMIC) Object[] args) throws Throwable {

            AdviceContextImpl context = PatchDispatcher.enterConstant(owner, null, args, beforeChain);

            //Assign the args back in to the method, which applies any changes made to them
            args = context.getArgs();

            return context;
        }
    }

    /** Constructor variant of Plain. */
    public static final class ConstructorPlain {
        @Advice.OnMethodEnter
        public static AdviceContextImpl enter(
                @Advice.Origin Class<?> owner,
                @Advice.AllArguments(typing = Assigner.Typing.DYNAMIC) Object[] args) {

            return PatchDispatcher.createContext(owner, null, args);
        }
    }
}
