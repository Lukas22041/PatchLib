package patchlib.agent.context;

import patchlib.api.context.Context;
import patchlib.api.query.FieldQuery;
import patchlib.api.query.MethodQuery;
import patchlib.api.ref.MethodRef;
import patchlib.api.ref.Ref;
import patchlib.api.store.PatchData;

/** Shared base for every context type. Holds the patched method's instance and arguments, and implements the
 * reflection and data utilities that all contexts expose. PatchContext and the redirect contexts extend this. */
public abstract class BaseContext implements Context {

    protected Class<?> owner;
    protected Object self;
    protected final Object[] args;

    protected BaseContext(Class<?> owner, Object self, Object[] args) {
        //Should always use the class of the actual instance, but @Before on Constructors and static methods have no instance, so in those cases the class of the patched method is used.
        this.owner = (self != null) ? self.getClass() : owner;
        this.self = self;
        this.args = args;
    }

    /** Required for Constructors */
    public void setSelf(Object self) {
        this.self = self;
    }

    public Object getSelf() {
        return self;
    }

    public <T> T getInferredSelf() {
        return (T) self;
    }

    public Object[] getArgs() {
        return args;
    }

    public Object getArg(int index) {
        return args[index];
    }

    public <T> Ref<T> getField(FieldQuery query) { return PatchReflection.field(owner, self, query); }
    public <T> Ref<T> getField(FieldQuery query, Object instance) { return PatchReflection.field(instance.getClass(), instance, query); }
    public <T> Ref<T> getField(String name) { return PatchReflection.field(owner, self, FieldQuery.named(name)); }
    public <T> Ref<T> getField(String name, Object instance) { return PatchReflection.field(instance.getClass(), instance, FieldQuery.named(name)); }

    public MethodRef getMethod(MethodQuery query) { return PatchReflection.method(owner, self, query); }
    public MethodRef getMethod(MethodQuery query, Object instance) { return PatchReflection.method(instance.getClass(), instance, query); }
    public MethodRef getMethod(String name) { return PatchReflection.method(owner, self, MethodQuery.named(name)); }
    public MethodRef getMethod(String name, Object instance) { return PatchReflection.method(instance.getClass(), instance, MethodQuery.named(name)); }

    public boolean hasMethod(MethodQuery query) { return PatchReflection.hasMethod(owner, query); }
    public boolean hasMethod(MethodQuery query, Object instance) { return PatchReflection.hasMethod(instance.getClass(), query); }
    public boolean hasMethod(String name) { return PatchReflection.hasMethod(owner, MethodQuery.named(name)); }
    public boolean hasMethod(String name, Object instance) { return PatchReflection.hasMethod(instance.getClass(), MethodQuery.named(name)); }

    public boolean hasField(FieldQuery query) { return PatchReflection.hasField(owner, query); }
    public boolean hasField(FieldQuery query, Object instance) { return PatchReflection.hasField(instance.getClass(), query); }
    public boolean hasField(String name) { return PatchReflection.hasField(owner, FieldQuery.named(name)); }
    public boolean hasField(String name, Object instance) { return PatchReflection.hasField(instance.getClass(), FieldQuery.named(name)); }

    /**A transient data store for per-instance data This data is not stored in the save. It is shared across all patches with access to this instance.
     * Useful for communicating across patches, or if something like a timer is needed. It should use unique keys, not something generic like "target" which multiple mods may use.
     * Throws an IllegalStateException if used on a static method or in @Before on a constructor method, as they have no instance data.  */
    public PatchData getData() {
        return new PatchData(PatchStore.getOrCreate(getSelf()));
    }
}
