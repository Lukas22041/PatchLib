package patchlib.agent.context;

import patchlib.api.context.FieldReadContext;

import java.lang.invoke.MethodHandle;

public class FieldReadContextImpl extends BaseContextImpl implements FieldReadContext {

    private final Object owner;
    private final MethodHandle nextLayer;

    private Object result = null;

    public FieldReadContextImpl(Class<?> effectiveClass, Object self, Object[] args, Object owner, MethodHandle nextLayer) {
        super(effectiveClass, self, args);
        this.owner = owner;
        this.nextLayer = nextLayer;
    }

    public Object getResult() {
        return result;
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
    public Object read() {
        try {
            return nextLayer.invokeExact(owner, self, this.args);
        } catch (Throwable ex) {
            throw uncheckedThrow(ex);
        }
    }

    @Override
    public void setResult(Object result) {
        this.result = result;
    }

    @SuppressWarnings("unchecked")
    private <T extends Throwable> T uncheckedThrow(Throwable ex) throws T {
        throw (T) ex;
    }
}
