package patchlib.agent.patch;

import net.bytebuddy.asm.MemberSubstitution;
import patchlib.agent.dispatch.DispatchIdMarker;
import patchlib.agent.dispatch.DispatchOwnerMarker;
import patchlib.agent.dispatch.PatchDispatcher;

import java.lang.invoke.MethodHandle;

/** Delegation targets that MemberSubstitution calls in place of an intercepted call or field access. These are the
 * redirect counterpart of the Advice templates. The bindings split the intercepted call's own values
 * (SUBSTITUTED_ELEMENT) from the host method's values (ENCLOSING_METHOD); SelfCallHandle is the original call. */
public final class RedirectBridges {

    public static Object methodCall(
            @DispatchIdMarker int siteId,
            @DispatchOwnerMarker Class<?> hostOwner,
            @MemberSubstitution.SelfCallHandle(bound = false) MethodHandle original,
            @MemberSubstitution.This(source = MemberSubstitution.Source.SUBSTITUTED_ELEMENT, optional = true) Object callReceiver,
            @MemberSubstitution.AllArguments(source = MemberSubstitution.Source.SUBSTITUTED_ELEMENT) Object[] callArgs,
            @MemberSubstitution.This(source = MemberSubstitution.Source.ENCLOSING_METHOD, optional = true) Object hostSelf,
            @MemberSubstitution.AllArguments(source = MemberSubstitution.Source.ENCLOSING_METHOD) Object[] hostArgs
    ) throws Throwable {
        return PatchDispatcher.redirectMethodCall(siteId, hostOwner, original, callReceiver, callArgs, hostSelf, hostArgs);
    }

    public static Object fieldRead(
            @DispatchIdMarker int siteId,
            @DispatchOwnerMarker Class<?> hostOwner,
            @MemberSubstitution.SelfCallHandle(bound = false) MethodHandle original,
            @MemberSubstitution.This(source = MemberSubstitution.Source.SUBSTITUTED_ELEMENT, optional = true) Object fieldOwner,
            @MemberSubstitution.This(source = MemberSubstitution.Source.ENCLOSING_METHOD, optional = true) Object hostSelf,
            @MemberSubstitution.AllArguments(source = MemberSubstitution.Source.ENCLOSING_METHOD) Object[] hostArgs
    ) throws Throwable {
        return PatchDispatcher.redirectFieldRead(siteId, hostOwner, original, fieldOwner, hostSelf, hostArgs);
    }

    public static void fieldWrite(
            @DispatchIdMarker int siteId,
            @DispatchOwnerMarker Class<?> hostOwner,
            @MemberSubstitution.SelfCallHandle(bound = false) MethodHandle original,
            @MemberSubstitution.This(source = MemberSubstitution.Source.SUBSTITUTED_ELEMENT, optional = true) Object fieldOwner,
            @MemberSubstitution.AllArguments(source = MemberSubstitution.Source.SUBSTITUTED_ELEMENT) Object[] writeArgs,
            @MemberSubstitution.This(source = MemberSubstitution.Source.ENCLOSING_METHOD, optional = true) Object hostSelf,
            @MemberSubstitution.AllArguments(source = MemberSubstitution.Source.ENCLOSING_METHOD) Object[] hostArgs
    ) throws Throwable {
        Object value = writeArgs.length > 0 ? writeArgs[0] : null;
        PatchDispatcher.redirectFieldWrite(siteId, hostOwner, original, fieldOwner, value, hostSelf, hostArgs);
    }
}
