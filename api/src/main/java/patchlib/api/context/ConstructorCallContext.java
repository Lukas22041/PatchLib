package patchlib.api.context;

import patchlib.api.ref.Ref;

/** Context for a @RedirectConstructorCall intercepting a constructor call inside the host method. The inherited Context methods
 * (getSelf, getArgs, reflection utilities, ...) refer to the host method; the methods in this interface refer to the construction. */
public interface ConstructorCallContext extends Context {

    /** The arguments passed to the intercepted construction. Writing to a spot in the array changes what call() passes on. */
    Object[] getCallArgs();

    /** A single argument of the intercepted construction. */
    Object getCallArg(int index);

    /** Writes a new value to an argument of the intercepted construction. */
    void setCallArg(int index, Object newValue);

    /** A typed read/writeable reference to an argument of the intercepted construction. */
    <T> Ref<T> getCallArgRef(int index);

    /** Calls the next layer down, or the real construction if this is the innermost layer, using the current call args.
     * Returns the resulting instance. This does not by itself become this layer's result, use setResult for that. */
    Object call();

    /** Same as call() but uses the given arguments instead of the current call args. */
    Object call(Object... args);

    /** Sets the instance this layer yields in place of the construction. Must be set, and be of the instantiated
     * type or a subtype. */
    void setResult(Object result);
}
