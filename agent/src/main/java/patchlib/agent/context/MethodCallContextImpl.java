package patchlib.agent.context;

import patchlib.api.context.MethodCallContext;
import patchlib.api.ref.Ref;

public class MethodCallContextImpl extends BaseContextImpl implements MethodCallContext {



    public MethodCallContextImpl(Class<?> owner, Object self, Object[] args) {
        super(owner, self, args);
    }

    /**
     * The arguments passed to the intercepted call. Writing to a spot in the array changes what call() passes on.
     */
    @Override
    public Object[] getCallArgs() {
        return new Object[0];
    }

    /**
     * A single argument of the intercepted call.
     *
     * @param index
     */
    @Override
    public Object getCallArg(int index) {
        return null;
    }

    /**
     * Writes a new value to an argument of the intercepted call.
     *
     * @param index
     * @param newValue
     */
    @Override
    public void setCallArg(int index, Object newValue) {

    }

    /**
     * A typed read/writeable reference to an argument of the intercepted call.
     *
     * @param index
     */
    @Override
    public <T> Ref<T> getCallArgRef(int index) {
        return null;
    }

    /**
     * The instance the call is made on. Null for a static call.
     */
    @Override
    public Object getCallReceiver() {
        return null;
    }

    /**
     * The instance the call is made on, cast to the type you assign it to. Null for a static call.
     */
    @Override
    public <T> T getInferredCallReceiver() {
        return null;
    }

    /**
     * Calls the next layer down, or the original call if this is the innermost layer, using the current call args.
     * Returns that call's result. This does not by itself become this layer's result, use setResult for that.
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
     * Sets the value this layer returns in place of the call. Must be set on a value returning call.
     *
     * @param result
     */
    @Override
    public void setResult(Object result) {

    }
}
