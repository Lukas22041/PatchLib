package patchlib.api.context;

import patchlib.api.ref.Ref;

/** Context for a @RedirectCall intercepting a method call inside the host method. The inherited Context methods
 * (getSelf, getArgs, reflection utilities, ...) refer to the host method; the methods below refer to the call. */
public interface MethodCallContext extends Context {

    /** The arguments passed to the intercepted call. Writing to a spot in the array changes what call() passes on. */
    Object[] getCallArgs();

    /** A single argument of the intercepted call. */
    Object getCallArg(int index);

    /** Writes a new value to an argument of the intercepted call. */
    void setCallArg(int index, Object newValue);

    /** A typed read/writeable reference to an argument of the intercepted call. */
    <T> Ref<T> getCallArgRef(int index);

    /** The instance the call is made on. Null for a static call. */
    Object getCallReceiver();

    /** The instance the call is made on, cast to the type you assign it to. Null for a static call. */
    <T> T getInferredCallReceiver();

    /** Calls the next layer down, or the original call if this is the innermost layer, using the current call args.
     * Returns that call's result. This does not by itself become this layer's result, use setResult for that. */
    Object call();

    /** Same as call() but uses the given arguments instead of the current call args. */
    Object call(Object... args);

    /** Sets the value this layer returns in place of the call. Must be set on a value returning call. */
    void setResult(Object result);
}
