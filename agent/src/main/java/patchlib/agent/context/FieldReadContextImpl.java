package patchlib.agent.context;

import patchlib.api.context.FieldReadContext;

public class FieldReadContextImpl extends BaseContextImpl implements FieldReadContext {
    /**
     * The instance whose field is read. Null for a static field.
     */
    @Override
    public Object getFieldOwner() {
        return null;
    }

    /**
     * The instance whose field is read, cast to the type you assign it to. Null for a static field.
     */
    @Override
    public <T> T getInferredFieldOwner() {
        return null;
    }

    /**
     * Reads the field at the next layer down, or the original field if this is the innermost layer.
     * Returns the read value. This does not by itself become this layer's result, use setResult for that.
     */
    @Override
    public Object read() {
        return null;
    }

    /**
     * Sets the value this read yields to the host method. Must be set.
     *
     * @param result
     */
    @Override
    public void setResult(Object result) {

    }
}
