package patchlib.agent.context;

import patchlib.api.context.FieldReadContext;

import java.lang.invoke.MethodHandle;

public class FieldReadContextImpl extends BaseContextImpl implements FieldReadContext {

    private final Object owner;
    private final MethodHandle next;

    private Object result = null;

    public FieldReadContextImpl(Class<?> effectiveClass, Object self, Object[] args, Object owner, MethodHandle next) {
        super(effectiveClass, self, args);
        this.owner = owner;
        this.next = next;
    }

    public Object getResult() {
        return result;
    }

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
