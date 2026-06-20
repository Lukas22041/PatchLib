package patch_lib.api;

import patch_lib.api.query.FieldQuery;
import patch_lib.api.query.MethodQuery;
import patch_lib.api.ref.ArgRef;
import patch_lib.api.ref.MethodRef;
import patch_lib.api.ref.Ref;
import patch_lib.api.store.PatchStore;

import java.util.Map;

public class PatchContext {

    private Class<?> owner;
    private Object self;
    private final Object[] args;
    private Object returnValue;
    private boolean skipOriginal;

    public PatchContext(Class<?> owner, Object self, Object[] args) {
        //Should always use the class of the actual instance, but @Before on Constructors and static methods have no instance, so in those cases the class of the patched method is used.
        this.owner = (self != null) ? self.getClass() : owner;
        this.self = self;
        this.args = args;
    }

    public Object getSelf() { return self; }
    public void setSelf(Object self) { this.self = self; }   // constructor template only (self isn't available on enter)

    public Object[] getArgs() { return args; }
    public Object getArg(int index) { return args[index]; }
    public void setArg(int index, Object newValue) { args[index] = newValue; }

    public Object getReturnValue() { return returnValue; }
    public void setReturnValue(Object newReturnValue) { this.returnValue = newReturnValue; }

    public boolean isSkipOriginal() { return skipOriginal; }
    /** Skip the original body and use this as the return value. Does not have an effect on constructors*/
    public void skipOriginal(Object returnValue) { this.skipOriginal = true; this.returnValue = returnValue; }

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

    public boolean hasMethod(MethodQuery query) { return PatchReflection.hasMethod(owner, query); }

    public boolean hasMethod(Object instance, MethodQuery query) { return PatchReflection.hasMethod(instance.getClass(), query); }

    public boolean hasField(FieldQuery query) { return PatchReflection.hasField(owner, query); }

    public boolean hasField(Object instance, FieldQuery query) { return PatchReflection.hasField(instance.getClass(), query); }

    /**A transient data store for per-instance data This data is not stored in the save. It is shared across all patches with access to this instance.
     * Useful for communicating across patches, or if something like a timer is needed. It should use unique keys, not something generic like "target" which multiple mods may use.
     * Throws an IllegalStateException if used on a static method or in @Before on a constructor method, as they have no instance data.  */
    public Map<String, Object> getData() {
        return PatchStore.getOrCreate(getSelf());
    }
}
