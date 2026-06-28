package patchlib.api;

import patchlib.api.query.FieldQuery;
import patchlib.api.query.MethodQuery;
import patchlib.api.ref.ArgRef;
import patchlib.api.ref.MethodRef;
import patchlib.api.ref.Ref;
import patchlib.api.store.PatchData;
import patchlib.api.store.PatchStore;

public class PatchContext implements BeforeContext, AfterContext, ExceptContext {

    private Class<?> owner;
    private Object self;
    private final Object[] args;
    private Object returnValue;
    private boolean skipOriginal;
    private Throwable thrown;
    private boolean suppress;
    private boolean allowSuppress = true;

    public PatchContext(Class<?> owner, Object self, Object[] args) {
        //Should always use the class of the actual instance, but @Before on Constructors and static methods have no instance, so in those cases the class of the patched method is used.
        this.owner = (self != null) ? self.getClass() : owner;
        this.self = self;
        this.args = args;
    }

    /** Get the instance that the patched method is called on. Can be null on @Before constructors and on all static methods */
    public Object getSelf() {
        return self;
    }

    /** Get the instance that the patched method is called on. Can be null on @Before constructors and on all static methods
     * Automatically casts the value to the variable/method parameters type that you call it for. */
    public <T> T getInferredSelf() {
        return (T) self;
    }

    /** Required for Constructors */
    public void setSelf(Object self) {
        this.self = self;
    }

    /**Gets the passed in arguments. Writing to a spot in the array will replace the original value */
    public Object[] getArgs() {
        return args;
    }

    /**Gets a read-only argument from the passed in arguments of the method.
     * While you can not replace the instance itself with this, you can still modify the member variables of the object. */
    public Object getArg(int index) {
        return args[index];
    }

    /** Writes a new value to an arg */
    public void setArg(int index, Object newValue) {
        args[index] = newValue;
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
        if (!allowSuppress) {
            throw new IllegalStateException("Can not suppress an exception thrown from a constructor, as it would leave a partially constructed object. Use replaceThrown or let the exception propagate instead.");
        }
        this.thrown = null;
        this.suppress = true;
        this.returnValue = returnValue;
    }

    public void setAllowSuppress(boolean allowSuppress) {
        this.allowSuppress = allowSuppress;
    }

    /** Checks if another patch already supressed the exception */
    public boolean isSuppressed() {
        return suppress;
    }

    /** Utility for retrieving a typed read/writeable arg of the original called method.
     * Changing an arg in a @Before patch means that the original method will be called and use the modified arguments. */
    public <T> Ref<T> getArgRef(int index) {
        return new ArgRef<>(args, index);
    }

    /** Reflection utility for reading/writing a typed field from the instance. Most useful for private members of a class, since reflection is otherwise blocked. First match wins. */
    public <T> Ref<T> getField(FieldQuery query) { return PatchReflection.field(owner, self, query); }

    /** Reflection utility for reading/writing a typed field from the given object. Most useful for private members of a class, since reflection is otherwise blocked. First match wins. */
    public <T> Ref<T> getField(Object instance, FieldQuery query) { return PatchReflection.field(instance.getClass(), instance, query); }


    /** Reflection utility for receiving a method from the instance. Most useful for private members of a class, since reflection is otherwise blocked. First match wins. */
    public MethodRef getMethod(MethodQuery query) { return PatchReflection.method(owner, self, query); }

    /** Reflection utility for receiving a method from the given object. Most useful for private members of a class, since reflection is otherwise blocked. First match wins. */
    public MethodRef getMethod(Object instance, MethodQuery query) { return PatchReflection.method(instance.getClass(), instance, query); }

    /** Reflection utility for checking if the patched class has a specific method */
    public boolean hasMethod(MethodQuery query) { return PatchReflection.hasMethod(owner, query); }

    /** Reflection utility for checking if a specific instance has a specific method */
    public boolean hasMethod(Object instance, MethodQuery query) { return PatchReflection.hasMethod(instance.getClass(), query); }

    /** Reflection utility for checking if the patched class has a specific field */
    public boolean hasField(FieldQuery query) { return PatchReflection.hasField(owner, query); }

    /** Reflection utility for checking if a specific instance has a specific field */
    public boolean hasField(Object instance, FieldQuery query) { return PatchReflection.hasField(instance.getClass(), query); }

    /**A transient data store for per-instance data This data is not stored in the save. It is shared across all patches with access to this instance.
     * Useful for communicating across patches, or if something like a timer is needed. It should use unique keys, not something generic like "target" which multiple mods may use.
     * Throws an IllegalStateException if used on a static method or in @Before on a constructor method, as they have no instance data.  */
    public PatchData getData() {
        return new PatchData(PatchStore.getOrCreate(getSelf()));
    }
}
