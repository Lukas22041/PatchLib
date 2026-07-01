package patchlib.agent.context;

import patchlib.api.context.AfterContext;
import patchlib.api.context.BeforeContext;
import patchlib.api.context.ExceptContext;
import patchlib.api.ref.ArgRef;
import patchlib.api.ref.Ref;

/** The context handed to @Before, @After and @Except patches. Adds the return value, skip and exception state on top
 * of the shared instance/argument/reflection utilities in BaseContext. Advice runs inside the patched method, so only
 * this context can replace its arguments; the redirect contexts observe them read-only. */
public class PatchContext extends BaseContext implements BeforeContext, AfterContext, ExceptContext {

    private Object returnValue;
    private boolean skipOriginal;
    private Throwable thrown;
    private boolean suppress;

    public PatchContext(Class<?> owner, Object self, Object[] args) {
        super(owner, self, args);
    }

    public void setArg(int index, Object newValue) {
        args[index] = newValue;
    }

    public <T> Ref<T> getArgRef(int index) {
        return new ArgRef<>(args, index);
    }

    /** Retrieves the return value from the original method  */
    public Object getReturnValue() {
        return returnValue;
    }

    /** Retrieves the return value from the original method. If multiple patches run on the same method, this can hold another patches return value.
     * Automatically casts the value to the variable/method parameters type that you call it for. */
    public <T> T getInferredReturnValue() {
        return (T) returnValue;
    }

    /** Replaces the return value from the original method. */
    public void setReturnValue(Object newReturnValue) {
        this.returnValue = newReturnValue;
    }

    /** Checks if something has skipped the original method in @Before */
    public boolean isSkipOriginal() {
        return skipOriginal;
    }

    /** Skip the original body and use this as the return value. Does not have an effect on constructors. Use "null" for void methods. */
    public void skipOriginal(Object returnValue) {
        this.skipOriginal = true;
        this.returnValue = returnValue;
    }

    /** Gets the exception that was thrown on the patched method. Can be null if another patch supressed the exception, and can also be another patches replaced exception. */
    public Throwable getThrown() { return thrown; }

    public void initThrown(Throwable thrown) {
        this.thrown = thrown; this.suppress = false;
    }

    /** Replaces the thrown exception with your own */
    public void replaceThrown(Throwable newThrown) {
        this.thrown = newThrown; this.suppress = false;
    }

    /** Suppresses an exception, requires passing a return value. Use "null" for void methods.
     * This method will throw an IllegalStateException if used in a constructor. */
    public void suppressException(Object returnValue) {
        this.thrown = null;
        this.suppress = true;
        this.returnValue = returnValue;
    }

    /** Checks if another patch already supressed the exception */
    public boolean isSuppressed() {
        return suppress;
    }
}
