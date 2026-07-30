package patchlib.agent.patch.redirect;

import net.bytebuddy.asm.MemberSubstitution;

import java.lang.invoke.MethodHandle;
import java.lang.reflect.Method;

/** The methods that MemberSubstitution.Substitution.Chain.Step.ForDelegation replaces the original call with, which then delegate further to the created redirect handles*/
public class RedirectDelegates {

    /** Method objects that the factory can refer to */
    protected static final Method METHOD_CALL_DELEGATE = getMethod("methodCallDelegate", MethodHandle.class, Object.class, Object[].class, Object.class, Object[].class);
    protected static final Method METHOD_CALL_STATIC_DELEGATE = getMethod("methodCallStaticDelegate", MethodHandle.class, Object.class, Object[].class, Object.class, Object[].class);
    protected static final Method CONSTRUCTOR_CALL_DELEGATE = getMethod("constructorCallDelegate", MethodHandle.class, Object[].class, Object.class, Object[].class);
    protected static final Method FIELD_READ_DELEGATE = getMethod("fieldReadDelegate", MethodHandle.class, Object.class, Object.class, Object[].class);
    protected static final Method FIELD_WRITE_DELEGATE = getMethod("fieldWriteDelegate", MethodHandle.class, Object.class, Object.class, Object.class, Object[].class);

    public static Object methodCallDelegate(
            @RedirectHandleMarker MethodHandle handle,
            @MemberSubstitution.This(source = MemberSubstitution.Source.SUBSTITUTED_ELEMENT, optional = true) Object callReceiver,
            @MemberSubstitution.AllArguments(source = MemberSubstitution.Source.SUBSTITUTED_ELEMENT) Object[] callArgs,
            @MemberSubstitution.This(source = MemberSubstitution.Source.ENCLOSING_METHOD, optional = true) Object hostSelf,
            @MemberSubstitution.AllArguments(source = MemberSubstitution.Source.ENCLOSING_METHOD) Object[] hostArgs
    ) throws Throwable {
        return handle.invokeExact(callReceiver, callArgs, hostSelf, hostArgs);
    }

    /** There's an issue where bytebuddy internally creates an array with -1 size when the method is static and has no input parameters,
     * setting includeSelf to true fixes this, without actually putting in the receiver in to the args list.*/
    public static Object methodCallStaticDelegate(
            @RedirectHandleMarker MethodHandle handle,
            @MemberSubstitution.This(source = MemberSubstitution.Source.SUBSTITUTED_ELEMENT, optional = true) Object callReceiver,
            @MemberSubstitution.AllArguments(source = MemberSubstitution.Source.SUBSTITUTED_ELEMENT, includeSelf = true) Object[] callArgs,
            @MemberSubstitution.This(source = MemberSubstitution.Source.ENCLOSING_METHOD, optional = true) Object hostSelf,
            @MemberSubstitution.AllArguments(source = MemberSubstitution.Source.ENCLOSING_METHOD) Object[] hostArgs
            ) throws Throwable {
        return handle.invokeExact(callReceiver, callArgs, hostSelf, hostArgs);
    }

    /** Constructors have the same issue with the -1 array size, so the includeSelf is checked to true here as well */
    public static Object constructorCallDelegate(
            @RedirectHandleMarker MethodHandle handle,
            @MemberSubstitution.AllArguments(source = MemberSubstitution.Source.SUBSTITUTED_ELEMENT, includeSelf = true) Object[] constructorArgs,
            @MemberSubstitution.This(source = MemberSubstitution.Source.ENCLOSING_METHOD, optional = true) Object hostSelf,
            @MemberSubstitution.AllArguments(source = MemberSubstitution.Source.ENCLOSING_METHOD) Object[] hostArgs
    ) throws Throwable {
        return handle.invokeExact(constructorArgs, hostSelf, hostArgs);
    }

    public static Object fieldReadDelegate(
            @RedirectHandleMarker MethodHandle handle,
            @MemberSubstitution.This(source = MemberSubstitution.Source.SUBSTITUTED_ELEMENT, optional = true) Object owner,
            @MemberSubstitution.This(source = MemberSubstitution.Source.ENCLOSING_METHOD, optional = true) Object hostSelf,
            @MemberSubstitution.AllArguments(source = MemberSubstitution.Source.ENCLOSING_METHOD) Object[] hostArgs
    ) throws Throwable {
        return handle.invokeExact(owner, hostSelf, hostArgs);
    }

    public static void fieldWriteDelegate(
            @RedirectHandleMarker MethodHandle handle,
            @MemberSubstitution.This(source = MemberSubstitution.Source.SUBSTITUTED_ELEMENT, optional = true) Object owner,
            @MemberSubstitution.Argument(source = MemberSubstitution.Source.SUBSTITUTED_ELEMENT, value = 0) Object writtenValue,
            @MemberSubstitution.This(source = MemberSubstitution.Source.ENCLOSING_METHOD, optional = true) Object hostSelf,
            @MemberSubstitution.AllArguments(source = MemberSubstitution.Source.ENCLOSING_METHOD) Object[] hostArgs
    ) throws Throwable {
        handle.invokeExact(owner, writtenValue, hostSelf, hostArgs);
    }

    private static Method getMethod(String methodName, Class<?>... parameters) {
        try {
            return RedirectDelegates.class.getMethod(methodName, parameters);
        } catch (NoSuchMethodException ex) {
            throw new RuntimeException("Could not find delegate method", ex);
        }
    }
}
