package patchlib.agent.context;

import patchlib.agent.patch.advice.AdvicePatchRegistry;
import patchlib.api.context.AfterContext;
import patchlib.api.context.BeforeContext;
import patchlib.api.context.ExceptContext;
import patchlib.api.ref.ArgRef;
import patchlib.api.ref.Ref;

import java.util.HashMap;
import java.util.Map;

public class HookContextImpl extends BaseContextImpl implements BeforeContext, AfterContext, ExceptContext {

    private final int siteId;

    private Object returnValue;
    private boolean skipOriginal;
    private Throwable thrown;
    private boolean suppress;

    //Created lazily on first use
    private Map<String, Object> locals;

    public HookContextImpl(Class<?> owner, Object self, Object[] args, int siteId) {
        super(owner, self, args);
        this.siteId = siteId;
    }

    public void initThrown(Throwable thrown) {
        this.thrown = thrown;
        this.suppress = false;
    }

    /**
     * Retrieves the return value from the original method
     */
    @Override
    public Object getReturnValue() {
        return returnValue;
    }

    /**
     * Retrieves the return value from the original method. If multiple patches run on the same method, this can hold another patches return value.
     * Automatically casts the value to the variable/method parameters type that you call it for.
     */
    @Override
    public <T> T getInferredReturnValue() {
        return (T) returnValue;
    }

    /**
     * Replaces the return value from the original method.
     *
     * @param newReturnValue
     */
    @Override
    public void setReturnValue(Object newReturnValue) {
        this.returnValue = newReturnValue;
    }

    /**
     * Skip the original body and use this as the return value. Does not have an effect on constructors. Use "null" for void methods.
     *
     * @param returnValue
     */
    @Override
    public void skipOriginal(Object returnValue) {
        this.skipOriginal = true;
        this.returnValue = returnValue;
    }

    /**
     * Gets the exception that was thrown on the patched method. Can be null if another patch suppressed the exception, and can also be another patches replaced exception.
     */
    @Override
    public Throwable getThrown() {
        return thrown;
    }

    /**
     * Replaces the thrown exception with your own
     *
     * @param newThrown
     */
    @Override
    public void replaceThrown(Throwable newThrown) {
        this.thrown = newThrown;
        this.suppress = false;
    }

    /**
     * Suppresses an exception, requires passing a return value. Use "null" for void methods.
     *
     * @param returnValue
     */
    @Override
    public void suppressException(Object returnValue) {
        this.suppress = true;
        this.thrown = null;
        this.returnValue = returnValue;
    }

    /**
     * Checks if another patch already suppressed the exception
     */
    @Override
    public boolean isSuppressed() {
        return suppress;
    }

    /**
     * Writes a new value to an arg
     *
     * @param index
     * @param newValue
     */
    @Override
    public void setArg(int index, Object newValue) {
        args[index] = newValue;
    }

    /**
     * Utility for retrieving a typed read/writeable arg of the original called method.
     * Changing an arg in a @Before patch means that the original method will be called and use the modified arguments.
     *
     * @param index
     */
    @Override
    public <T> Ref<T> getArgRef(int index) {
        return new ArgRef<>(args, index);
    }

    /**
     * Checks if something has skipped the original method in @Before
     */
    @Override
    public boolean isSkipOriginal() {
        return this.skipOriginal;
    }

    /**
     * Stores a value for the duration of this single method call, shared with all before/after/except patches.
     *
     * @param key
     * @param value
     */
    @Override
    public void setLocal(String key, Object value) {
        if (locals == null) locals = new HashMap<>();
        locals.put(key, value);
    }

    /**
     * Retrieves a value stored by setLocal during this method call. shared with all before/after/except patches.
     *
     * @param key
     */
    @Override
    public <T> T getLocal(String key) {
        if (locals == null) return null;
        return (T) locals.get(key);
    }

    /**
     * Returns true if the running method is the objects most-derived implementation of the method, also true for static methods and when there is no instance.
     * This is useful for when you are doing patches that target with "subtype". This is because those patches will patch both the derived and the inherited method,
     * potentially causing your patch to be called twice or more if the derived method has a super() call in it.
     */
    @Override
    public boolean isMostDerivedCall() {
        //Static methods
        if (self == null) return true;
        return AdvicePatchRegistry.getSite(siteId).isMostDerived(self.getClass());
    }
}
