package patchlib.agent.context;

import patchlib.api.context.ConstructorCallContext;
import patchlib.api.ref.ArgRef;
import patchlib.api.ref.Ref;

import java.lang.invoke.MethodHandle;

public class ConstructorCallContextImpl extends BaseContextImpl implements ConstructorCallContext {

    private final Object[] constructorArgs;
    private final MethodHandle nextLayer;

    private Object result = null;
    private Throwable thrownFromNextLayer;

    public ConstructorCallContextImpl(Class<?> effectiveClass, Object self, Object[] args, Object[] constructorArgs, MethodHandle nextLayer) {
        super(effectiveClass, self, args);
        this.constructorArgs = constructorArgs;
        this.nextLayer = nextLayer;
    }

    public Object getResult() {
        return result;
    }

    @Override
    public Object[] getCallArgs() {
        return constructorArgs;
    }

    @Override
    public Object getCallArg(int index) {
        return constructorArgs[index];
    }

    @Override
    public void setCallArg(int index, Object newValue) {
        constructorArgs[index] = newValue;
    }

    @Override
    public <T> Ref<T> getCallArgRef(int index) {
        return new ArgRef<>(constructorArgs, index);

    }

    @Override
    public Object call() {
        return call(constructorArgs);
    }

    @Override
    public Object call(Object... args) {
        try {
            return nextLayer.invokeExact(args, self, this.args);
        } catch (Throwable ex) {
            thrownFromNextLayer = ex;
            throw uncheckedThrow(ex);
        }
    }

    @Override
    public void setResult(Object result) {
        this.result = result;
    }

    public boolean isResponsibleForException(Throwable thrown) {
        return thrownFromNextLayer != thrown;
    }

    @SuppressWarnings("unchecked")
    private <T extends Throwable> T uncheckedThrow(Throwable ex) throws T {
        throw (T) ex;
    }
}
