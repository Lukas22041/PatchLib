package patchlib.agent.context;

import patchlib.api.context.FieldWriteContext;

import java.lang.invoke.MethodHandle;

public class FieldWriteContextImpl extends BaseContextImpl implements FieldWriteContext {

    private final Object owner;
    private Object valueToWrite;
    private final MethodHandle nextLayer;

    public FieldWriteContextImpl(Class<?> effectiveClass, Object self, Object[] args, Object owner, Object valueToWrite, MethodHandle nextLayer) {
        super(effectiveClass, self, args);
        this.owner = owner;
        this.valueToWrite = valueToWrite;
        this.nextLayer = nextLayer;
    }

    /**
     * The instance whose field is written. Null for a static field.
     */
    @Override
    public Object getFieldOwner() {
        return owner;
    }

    /**
     * The instance whose field is written, cast to the type you assign it to. Null for a static field.
     */
    @Override
    public <T> T getInferredFieldOwner() {
        return (T) owner;
    }

    /**
     * The value being written to the field.
     */
    @Override
    public Object getValueToWrite() {
        return valueToWrite;
    }

    /**
     * The value being written, cast to the type you assign it to.
     */
    @Override
    public <T> T getInferredValueToWrite() {
        return (T) valueToWrite;
    }

    /**
     * Performs the write at the next layer down, or the original write if this is the innermost layer, using the
     * current value. Skip the write entirely by never calling this.
     */
    @Override
    public void write() {
        write(valueToWrite);
    }

    /**
     * Same as write() but stores the given written value instead of the current one.
     *
     * @param valueToWrite
     */
    @Override
    public void write(Object valueToWrite) {
        try {
            nextLayer.invokeExact(owner, valueToWrite, self, this.args);
        } catch (Throwable ex) {
            throw uncheckedThrow(ex);
        }
    }

    @SuppressWarnings("unchecked")
    private <T extends Throwable> T uncheckedThrow(Throwable ex) throws T {
        throw (T) ex;
    }
}
