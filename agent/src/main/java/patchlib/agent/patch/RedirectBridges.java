package patchlib.agent.patch;

import net.bytebuddy.asm.MemberSubstitution;
import patchlib.agent.dispatch.DispatchIdMarker;
import patchlib.agent.dispatch.DispatchOwnerMarker;
import patchlib.agent.dispatch.PatchDispatcher;

import java.lang.invoke.MethodHandle;

/** Delegation targets that MemberSubstitution calls in place of an intercepted call or field access. These are the
 * redirect counterpart of the Advice templates. The bindings split the intercepted call's own values
 * (SUBSTITUTED_ELEMENT) from the host method's values (ENCLOSING_METHOD); SelfCallHandle is the original call.
 * Each bridge normalizes its kind specific values into the shared target/args shape of PatchDispatcher.redirect. */
public final class RedirectBridges {

    static final Object[] NO_ARGS = new Object[0];

    /** The receiver is absent for a static call. */
    public static Object methodCall(
            @DispatchIdMarker int siteId,
            @DispatchOwnerMarker Class<?> hostOwner,
            @MemberSubstitution.SelfCallHandle(bound = false) MethodHandle original,
            @MemberSubstitution.This(source = MemberSubstitution.Source.SUBSTITUTED_ELEMENT, optional = true) Object callReceiver,
            @MemberSubstitution.AllArguments(source = MemberSubstitution.Source.SUBSTITUTED_ELEMENT) Object[] callArgs,
            @MemberSubstitution.This(source = MemberSubstitution.Source.ENCLOSING_METHOD, optional = true) Object hostSelf,
            @MemberSubstitution.AllArguments(source = MemberSubstitution.Source.ENCLOSING_METHOD) Object[] hostArgs
    ) throws Throwable {
        return PatchDispatcher.redirect(siteId, hostOwner, original, callReceiver, callArgs, hostSelf, hostArgs);
    }

    /** methodCall variant for static calls. includeSelf works around a ByteBuddy bug where the argument lists
     * capacity hint goes negative for a zero arg static call. There is no receiver, so the arguments are unchanged. */
    public static Object methodCallStatic(
            @DispatchIdMarker int siteId,
            @DispatchOwnerMarker Class<?> hostOwner,
            @MemberSubstitution.SelfCallHandle(bound = false) MethodHandle original,
            @MemberSubstitution.This(source = MemberSubstitution.Source.SUBSTITUTED_ELEMENT, optional = true) Object callReceiver,
            @MemberSubstitution.AllArguments(source = MemberSubstitution.Source.SUBSTITUTED_ELEMENT, includeSelf = true) Object[] callArgs,
            @MemberSubstitution.This(source = MemberSubstitution.Source.ENCLOSING_METHOD, optional = true) Object hostSelf,
            @MemberSubstitution.AllArguments(source = MemberSubstitution.Source.ENCLOSING_METHOD) Object[] hostArgs
    ) throws Throwable {
        return PatchDispatcher.redirect(siteId, hostOwner, original, callReceiver, callArgs, hostSelf, hostArgs);
    }

    /** There is no receiver, the original handle allocates and initializes in one step. */
    public static Object constructorCall(
            @DispatchIdMarker int siteId,
            @DispatchOwnerMarker Class<?> hostOwner,
            @MemberSubstitution.SelfCallHandle(bound = false) MethodHandle original,
            //includeSelf works around a ByteBuddy bug where the argument lists capacity hint goes negative for a
            //zero arg constructor. A construction has no receiver, so the bound arguments are unchanged by it.
            @MemberSubstitution.AllArguments(source = MemberSubstitution.Source.SUBSTITUTED_ELEMENT, includeSelf = true) Object[] callArgs,
            @MemberSubstitution.This(source = MemberSubstitution.Source.ENCLOSING_METHOD, optional = true) Object hostSelf,
            @MemberSubstitution.AllArguments(source = MemberSubstitution.Source.ENCLOSING_METHOD) Object[] hostArgs
    ) throws Throwable {
        return PatchDispatcher.redirect(siteId, hostOwner, original, null, callArgs, hostSelf, hostArgs);
    }

    /** A read takes no arguments. */
    public static Object fieldRead(
            @DispatchIdMarker int siteId,
            @DispatchOwnerMarker Class<?> hostOwner,
            @MemberSubstitution.SelfCallHandle(bound = false) MethodHandle original,
            @MemberSubstitution.This(source = MemberSubstitution.Source.SUBSTITUTED_ELEMENT, optional = true) Object fieldOwner,
            @MemberSubstitution.This(source = MemberSubstitution.Source.ENCLOSING_METHOD, optional = true) Object hostSelf,
            @MemberSubstitution.AllArguments(source = MemberSubstitution.Source.ENCLOSING_METHOD) Object[] hostArgs
    ) throws Throwable {
        return PatchDispatcher.redirect(siteId, hostOwner, original, fieldOwner, NO_ARGS, hostSelf, hostArgs);
    }

    /** The single argument is the value being written. A write has no result, the dispatchers return value is discarded. */
    public static void fieldWrite(
            @DispatchIdMarker int siteId,
            @DispatchOwnerMarker Class<?> hostOwner,
            @MemberSubstitution.SelfCallHandle(bound = false) MethodHandle original,
            @MemberSubstitution.This(source = MemberSubstitution.Source.SUBSTITUTED_ELEMENT, optional = true) Object fieldOwner,
            @MemberSubstitution.AllArguments(source = MemberSubstitution.Source.SUBSTITUTED_ELEMENT) Object[] writeArgs,
            @MemberSubstitution.This(source = MemberSubstitution.Source.ENCLOSING_METHOD, optional = true) Object hostSelf,
            @MemberSubstitution.AllArguments(source = MemberSubstitution.Source.ENCLOSING_METHOD) Object[] hostArgs
    ) throws Throwable {
        PatchDispatcher.redirect(siteId, hostOwner, original, fieldOwner, writeArgs, hostSelf, hostArgs);
    }
}
