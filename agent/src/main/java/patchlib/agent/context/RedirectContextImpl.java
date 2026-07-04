package patchlib.agent.context;

import patchlib.agent.dispatch.PatchDispatcher;
import patchlib.api.context.ConstructorCallContext;
import patchlib.api.context.FieldReadContext;
import patchlib.api.context.FieldWriteContext;
import patchlib.api.context.MethodCallContext;
import patchlib.api.ref.ArgRef;
import patchlib.api.ref.Ref;

import java.lang.invoke.MethodHandle;

/** Runtime context for one layer of any redirect. The context interfaces are role specific views onto one shape:
 * a target (the call receiver or field owner, null for a construction), the values flowing into the access (call
 * args, the written value, or nothing for a read), a way to proceed, and a result flowing back out (unused by
 * writes). next is the chain link below this layer, a deeper layer or the original access, see ChainBootstrap.
 * The inherited BaseContext state is the host method. */
public final class RedirectContextImpl extends BaseContext
        implements MethodCallContext, ConstructorCallContext, FieldReadContext, FieldWriteContext {

    private final Object target;   //call receiver or field owner, null when static or a construction
    private final Object[] callArgs;   //call args, or [value] for a write, or empty for a read
    private final MethodHandle next;
    private Object result;
    private Throwable propagated;  //the exception that last surfaced from this context's proceed, see isPropagating

    public RedirectContextImpl(Class<?> hostOwner, Object hostSelf, Object[] hostArgs, Object target, Object[] callArgs,
                               MethodHandle next) {
        super(hostOwner, hostSelf, hostArgs);
        this.target = target;
        this.callArgs = callArgs;
        this.next = next;
    }

    public Object getCallReceiver() { return target; }
    public Object getFieldOwner() { return target; }
    public <T> T getInferredCallReceiver() { return (T) target; }
    public <T> T getInferredFieldOwner() { return (T) target; }

    public Object[] getCallArgs() { return callArgs; }
    public Object getCallArg(int index) { return callArgs[index]; }
    public void setCallArg(int index, Object newValue) { callArgs[index] = newValue; }
    public <T> Ref<T> getCallArgRef(int index) { return new ArgRef<>(callArgs, index); }

    public Object getValue() { return callArgs[0]; }
    public <T> T getInferredValue() { return (T) callArgs[0]; }
    public void setValue(Object value) { callArgs[0] = value; }
    public <T> Ref<T> getValueRef() { return new ArgRef<>(callArgs, 0); }

    public Object call() { return proceed(callArgs); }
    public Object call(Object... callArgs) { return proceed(callArgs); }
    public Object read() { return proceed(callArgs); }
    public void write() { proceed(callArgs); }
    public void write(Object value) { proceed(new Object[]{value}); }

    public void setResult(Object result) { this.result = result; }

    /** Read by the chain only, the handler proceeds with call()/read()/write() instead. */
    public Object getResult() { return result; }

    /** Used by the chain only, to tell an exception surfacing from proceed apart from one this layer threw itself. */
    public boolean isPropagating(Throwable t) { return propagated == t; }

    private Object proceed(Object[] proceedArgs) {
        try {
            return (Object) next.invokeExact(target, proceedArgs, self, args);
        } catch (Throwable t) {
            propagated = t;
            throw PatchDispatcher.uncheckedThrow(t);
        }
    }
}
