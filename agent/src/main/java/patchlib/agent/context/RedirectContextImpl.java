package patchlib.agent.context;

import patchlib.agent.dispatch.Operation;
import patchlib.agent.dispatch.PatchDispatcher;
import patchlib.api.context.FieldReadContext;
import patchlib.api.context.FieldWriteContext;
import patchlib.api.context.MethodCallContext;
import patchlib.api.ref.ArgRef;
import patchlib.api.ref.Ref;

/** Runtime context for one layer of any redirect. The three context interfaces are role specific views onto one shape:
 * a target (the call receiver or field owner), the values flowing into the access (call args, the written value, or
 * nothing for a read), a way to proceed to the next layer, and a result flowing back out (unused by writes). The
 * inherited BaseContext state is the host method. */
public final class RedirectContextImpl extends BaseContext
        implements MethodCallContext, FieldReadContext, FieldWriteContext {

    private final Object target;   //call receiver or field owner, null when static
    private final Object[] args;   //call args, or [value] for a write, or empty for a read
    private final Operation next;
    private Object result;

    public RedirectContextImpl(Class<?> hostOwner, Object hostSelf, Object[] hostArgs, Object target, Object[] args, Operation next) {
        super(hostOwner, hostSelf, hostArgs);
        this.target = target;
        this.args = args;
        this.next = next;
    }

    public Object getCallReceiver() { return target; }
    public Object getFieldOwner() { return target; }
    public <T> T getInferredCallReceiver() { return (T) target; }
    public <T> T getInferredFieldOwner() { return (T) target; }

    public Object[] getCallArgs() { return args; }
    public Object getCallArg(int index) { return args[index]; }
    public void setCallArg(int index, Object newValue) { args[index] = newValue; }
    public <T> Ref<T> getCallArgRef(int index) { return new ArgRef<>(args, index); }

    public Object getValue() { return args[0]; }
    public <T> T getInferredValue() { return (T) args[0]; }
    public void setValue(Object value) { args[0] = value; }
    public <T> Ref<T> getValueRef() { return new ArgRef<>(args, 0); }

    public Object call() { return proceed(args); }
    public Object call(Object... callArgs) { return proceed(callArgs); }

    public void setResult(Object result) { this.result = result; }

    /** Read by the dispatcher only, the handler proceeds with call() instead. */
    public Object getResult() { return result; }

    private Object proceed(Object[] proceedArgs) {
        try {
            return next.call(proceedArgs);
        } catch (Throwable t) {
            throw PatchDispatcher.propagate(t);
        }
    }
}
