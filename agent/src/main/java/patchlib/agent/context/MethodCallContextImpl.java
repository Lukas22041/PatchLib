package patchlib.agent.context;

import patchlib.api.context.MethodCallContext;
import patchlib.api.ref.ArgRef;
import patchlib.api.ref.Ref;

import java.lang.invoke.MethodHandle;

public class MethodCallContextImpl extends BaseContextImpl implements MethodCallContext {

    private final Object receiver;
    private final Object[] callArgs;
    private final MethodHandle nextLayer;

    private Object result = null;

    public MethodCallContextImpl(Class<?> effectiveClass, Object self, Object[] args, Object receiver, Object[] callArgs, MethodHandle nextLayer) {
        super(effectiveClass, self, args);
        this.receiver = receiver;
        this.callArgs = callArgs;
        this.nextLayer = nextLayer;
    }

    public Object getResult() {
        return result;
    }

    @Override
    public Object[] getCallArgs() {
        return callArgs;
    }

    @Override
    public Object getCallArg(int index) {
        return callArgs[index];
    }

    @Override
    public void setCallArg(int index, Object newValue) {
        callArgs[index] = newValue;
    }

    @Override
    public <T> Ref<T> getCallArgRef(int index) {
        return new ArgRef<>(callArgs, index);
    }

    @Override
    public Object getCallReceiver() {
        return receiver;
    }

    @Override
    public <T> T getInferredCallReceiver() {
        return (T) receiver;
    }

    @Override
    public Object call() {
        return call(callArgs);
    }

    @Override
    public Object call(Object... args) {
        try {
            return nextLayer.invokeExact(receiver, args, self, this.args);
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
