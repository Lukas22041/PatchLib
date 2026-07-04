package patchlib.agent.patch;

import net.bytebuddy.asm.MemberSubstitution;
import patchlib.agent.dispatch.RedirectChainMarker;

import java.lang.invoke.MethodHandle;

/** Delegation targets that MemberSubstitution calls in place of an intercepted call or field access. These are the
 * redirect counterpart of the Advice templates. The bindings split the intercepted call's own values
 * (SUBSTITUTED_ELEMENT) from the host method's values (ENCLOSING_METHOD). The site's composed layer chain arrives
 * as a dynamic constant, see ChainBootstrap.redirectBootstrap. Each bridge normalizes its kind specific values
 * into the shared chain shape: target, access args, host self, host args. */
public final class RedirectBridges {

    static final Object[] NO_ARGS = new Object[0];

    /** The receiver is absent for a static call. */
    public static Object methodCall(
            @RedirectChainMarker MethodHandle chain,
            @MemberSubstitution.This(source = MemberSubstitution.Source.SUBSTITUTED_ELEMENT, optional = true) Object callReceiver,
            @MemberSubstitution.AllArguments(source = MemberSubstitution.Source.SUBSTITUTED_ELEMENT) Object[] callArgs,
            @MemberSubstitution.This(source = MemberSubstitution.Source.ENCLOSING_METHOD, optional = true) Object hostSelf,
            @MemberSubstitution.AllArguments(source = MemberSubstitution.Source.ENCLOSING_METHOD) Object[] hostArgs
    ) throws Throwable {
        return chain.invokeExact(callReceiver, callArgs, hostSelf, hostArgs);
    }

    /** methodCall variant for static calls. includeSelf works around a ByteBuddy bug where the argument lists
     * capacity hint goes negative for a zero arg static call. There is no receiver, so the arguments are unchanged. */
    public static Object methodCallStatic(
            @RedirectChainMarker MethodHandle chain,
            @MemberSubstitution.This(source = MemberSubstitution.Source.SUBSTITUTED_ELEMENT, optional = true) Object callReceiver,
            @MemberSubstitution.AllArguments(source = MemberSubstitution.Source.SUBSTITUTED_ELEMENT, includeSelf = true) Object[] callArgs,
            @MemberSubstitution.This(source = MemberSubstitution.Source.ENCLOSING_METHOD, optional = true) Object hostSelf,
            @MemberSubstitution.AllArguments(source = MemberSubstitution.Source.ENCLOSING_METHOD) Object[] hostArgs
    ) throws Throwable {
        return chain.invokeExact(callReceiver, callArgs, hostSelf, hostArgs);
    }

    /** There is no receiver, the original handle allocates and initializes in one step. */
    public static Object constructorCall(
            @RedirectChainMarker MethodHandle chain,
            //includeSelf works around a ByteBuddy bug where the argument lists capacity hint goes negative for a
            //zero arg constructor. A construction has no receiver, so the bound arguments are unchanged by it.
            @MemberSubstitution.AllArguments(source = MemberSubstitution.Source.SUBSTITUTED_ELEMENT, includeSelf = true) Object[] callArgs,
            @MemberSubstitution.This(source = MemberSubstitution.Source.ENCLOSING_METHOD, optional = true) Object hostSelf,
            @MemberSubstitution.AllArguments(source = MemberSubstitution.Source.ENCLOSING_METHOD) Object[] hostArgs
    ) throws Throwable {
        return chain.invokeExact((Object) null, callArgs, hostSelf, hostArgs);
    }

    /** A read takes no arguments. */
    public static Object fieldRead(
            @RedirectChainMarker MethodHandle chain,
            @MemberSubstitution.This(source = MemberSubstitution.Source.SUBSTITUTED_ELEMENT, optional = true) Object fieldOwner,
            @MemberSubstitution.This(source = MemberSubstitution.Source.ENCLOSING_METHOD, optional = true) Object hostSelf,
            @MemberSubstitution.AllArguments(source = MemberSubstitution.Source.ENCLOSING_METHOD) Object[] hostArgs
    ) throws Throwable {
        return chain.invokeExact(fieldOwner, NO_ARGS, hostSelf, hostArgs);
    }

    /** The single argument is the value being written. A write has no result, the chains return value is discarded. */
    public static void fieldWrite(
            @RedirectChainMarker MethodHandle chain,
            @MemberSubstitution.This(source = MemberSubstitution.Source.SUBSTITUTED_ELEMENT, optional = true) Object fieldOwner,
            @MemberSubstitution.AllArguments(source = MemberSubstitution.Source.SUBSTITUTED_ELEMENT) Object[] writeArgs,
            @MemberSubstitution.This(source = MemberSubstitution.Source.ENCLOSING_METHOD, optional = true) Object hostSelf,
            @MemberSubstitution.AllArguments(source = MemberSubstitution.Source.ENCLOSING_METHOD) Object[] hostArgs
    ) throws Throwable {
        //invokeExact needs the chain's Object return in its call descriptor, the value is always null here.
        Object unused = chain.invokeExact(fieldOwner, writeArgs, hostSelf, hostArgs);
    }
}
