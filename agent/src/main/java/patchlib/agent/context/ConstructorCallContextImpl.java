package patchlib.agent.context;

import patchlib.api.context.ConstructorCallContext;
import patchlib.api.ref.Ref;

public class ConstructorCallContextImpl extends BaseContextImpl implements ConstructorCallContext {
    /**
     * The arguments passed to the intercepted construction. Writing to a spot in the array changes what call() passes on.
     */
    @Override
    public Object[] getCallArgs() {
        return new Object[0];
    }

    /**
     * A single argument of the intercepted construction.
     *
     * @param index
     */
    @Override
    public Object getCallArg(int index) {
        return null;
    }

    /**
     * Writes a new value to an argument of the intercepted construction.
     *
     * @param index
     * @param newValue
     */
    @Override
    public void setCallArg(int index, Object newValue) {

    }

    /**
     * A typed read/writeable reference to an argument of the intercepted construction.
     *
     * @param index
     */
    @Override
    public <T> Ref<T> getCallArgRef(int index) {
        return null;
    }

    /**
     * Calls the next layer down, or the real construction if this is the innermost layer, using the current call args.
     * Returns the resulting instance. This does not by itself become this layer's result, use setResult for that.
     */
    @Override
    public Object call() {
        return null;
    }

    /**
     * Same as call() but uses the given arguments instead of the current call args.
     *
     * @param args
     */
    @Override
    public Object call(Object... args) {
        return null;
    }

    /**
     * Sets the instance this layer yields in place of the construction. Must be set, and be of the instantiated
     * type or a subtype.
     *
     * @param result
     */
    @Override
    public void setResult(Object result) {

    }
}
