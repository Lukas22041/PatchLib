package patchlib.agent.context;

import patchlib.api.context.FieldWriteContext;

import java.lang.invoke.MethodHandle;

public class FieldWriteContextImpl extends BaseContextImpl implements FieldWriteContext {

    private final Object owner;
    private Object valueToWrite;
    private final MethodHandle nextLayer;

    private Throwable thrownFromNextLayer;

    public FieldWriteContextImpl(Class<?> effectiveClass, Object self, Object[] args, Object owner, Object valueToWrite, MethodHandle nextLayer) {
        super(effectiveClass, self, args);
        this.owner = owner;
        this.valueToWrite = valueToWrite;
        this.nextLayer = nextLayer;
    }

    @Override
    public Object getFieldOwner() {
        return owner;
    }

    @Override
    public <T> T getInferredFieldOwner() {
        return (T) owner;
    }

    @Override
    public Object getValueToWrite() {
        return valueToWrite;
    }

    @Override
    public <T> T getInferredValueToWrite() {
        return (T) valueToWrite;
    }

    @Override
    public void write() {
        write(valueToWrite);
    }

    @Override
    public void write(Object valueToWrite) {
        try {
            nextLayer.invokeExact(owner, valueToWrite, self, this.args);
        } catch (Throwable ex) {
            thrownFromNextLayer = ex;
            throw uncheckedThrow(ex);
        }
    }

    public boolean isResponsibleForException(Throwable thrown) {
        return thrownFromNextLayer != thrown;
    }

    @SuppressWarnings("unchecked")
    private <T extends Throwable> T uncheckedThrow(Throwable ex) throws T {
        throw (T) ex;
    }
}
