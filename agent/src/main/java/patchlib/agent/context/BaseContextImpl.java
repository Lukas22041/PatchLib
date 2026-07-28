package patchlib.agent.context;

import patchlib.agent.context.util.PatchReflection;
import patchlib.agent.context.util.PatchTransientDataStore;
import patchlib.api.context.Context;
import patchlib.api.query.FieldQuery;
import patchlib.api.query.MethodQuery;
import patchlib.api.ref.MethodRef;
import patchlib.api.ref.Ref;
import patchlib.api.spec.FieldQuerySpec;
import patchlib.api.spec.MethodQuerySpec;
import patchlib.api.store.PatchData;

public abstract class BaseContextImpl implements Context {

    protected Class<?> effectiveClass;
    protected Object self;
    protected final Object[] args;

    public BaseContextImpl(Class<?> effectiveClass, Object self, Object[] args) {
        this.effectiveClass = self != null ? self.getClass() : effectiveClass;
        this.self = self;
        this.args = args;
    }

    public void setSelf(Object self) {
        this.self = self;
        if (self != null) {
            this.effectiveClass = self.getClass();
        }
    }

    /**
     * Get the instance that the patched method is called on. Can be null on @Before constructors and on all static methods
     */
    @Override
    public Object getSelf() {
        return self;
    }

    /**
     * Get the instance that the patched method is called on. Can be null on @Before constructors and on all static methods
     * Automatically casts the value to the variable/method parameters type that you call it for.
     */
    @Override
    public <T> T getInferredSelf() {
        return (T) self;
    }

    /**
     * Gets the passed in arguments. In @Before/@After/@Except patches, writing to a spot in the array will replace the
     * original value. In redirects the host method's arguments are read-only, see HookContext.
     */
    @Override
    public Object[] getArgs() {
        return args;
    }

    /**
     * Gets a read-only argument from the passed in arguments of the method.
     * While you can not replace the instance itself with this, you can still modify the member variables of the object.
     *
     * @param index
     */
    @Override
    public Object getArg(int index) {
        return args[index];
    }

    /**
     * Reflection utility for reading/writing a typed field from the instance. Most useful for private members of a class, since reflection is otherwise blocked. First match wins.
     *
     * @param query
     */
    @Override
    public <T> Ref<T> getField(FieldQuerySpec query) {
        return PatchReflection.getField(effectiveClass, self, query);
    }

    /**
     * Reflection utility for reading/writing a typed field from the given object. Most useful for private members of a class, since reflection is otherwise blocked. First match wins.
     *
     * @param query
     * @param instance
     */
    @Override
    public <T> Ref<T> getField(FieldQuerySpec query, Object instance) {
        return PatchReflection.getField(instance.getClass(), instance, query);
    }

    /**
     * Reflection utility for reading/writing a typed field from the instance. Most useful for private members of a class, since reflection is otherwise blocked.
     *
     * @param name
     */
    @Override
    public <T> Ref<T> getField(String name) {
        return PatchReflection.getField(effectiveClass, self, FieldQuery.create().fieldName(name).build());
    }

    /**
     * Reflection utility for reading/writing a typed field from the given object. Most useful for private members of a class, since reflection is otherwise blocked.
     *
     * @param name
     * @param instance
     */
    @Override
    public <T> Ref<T> getField(String name, Object instance) {
        return PatchReflection.getField(instance.getClass(), instance, FieldQuery.create().fieldName(name).build());
    }

    /**
     * Reflection utility for receiving a method from the instance. Most useful for private members of a class, since reflection is otherwise blocked. First match wins.
     *
     * @param query
     */
    @Override
    public MethodRef getMethod(MethodQuerySpec query) {
        return PatchReflection.getMethod(effectiveClass, self, query);
    }

    /**
     * Reflection utility for receiving a method from the given object. Most useful for private members of a class, since reflection is otherwise blocked. First match wins.
     *
     * @param query
     * @param instance
     */
    @Override
    public MethodRef getMethod(MethodQuerySpec query, Object instance) {
        return PatchReflection.getMethod(instance.getClass(), instance, query);
    }

    /**
     * Reflection utility for receiving a method from the instance. Most useful for private members of a class, since reflection is otherwise blocked. First match wins.
     *
     * @param name
     */
    @Override
    public MethodRef getMethod(String name) {
        return PatchReflection.getMethod(effectiveClass, self, MethodQuery.create().methodName(name).build());
    }

    /**
     * Reflection utility for receiving a method from the given object. Most useful for private members of a class, since reflection is otherwise blocked. First match wins.
     *
     * @param name
     * @param instance
     */
    @Override
    public MethodRef getMethod(String name, Object instance) {
        return PatchReflection.getMethod(instance.getClass(), instance, MethodQuery.create().methodName(name).build());
    }

    /**
     * Reflection utility for checking if the patched class has a specific method
     *
     * @param query
     */
    @Override
    public boolean hasMethod(MethodQuerySpec query) {
        return PatchReflection.hasMethod(effectiveClass, self, query);
    }

    /**
     * Reflection utility for checking if a specific instance has a specific method
     *
     * @param query
     * @param instance
     */
    @Override
    public boolean hasMethod(MethodQuerySpec query, Object instance) {
        return PatchReflection.hasMethod(instance.getClass(), instance, query);
    }

    /**
     * Reflection utility for checking if the patched class has a specific method
     *
     * @param name
     */
    @Override
    public boolean hasMethod(String name) {
        return PatchReflection.hasMethod(effectiveClass, self, MethodQuery.create().methodName(name).build());
    }

    /**
     * Reflection utility for checking if a specific instance has a specific method
     *
     * @param name
     * @param instance
     */
    @Override
    public boolean hasMethod(String name, Object instance) {
        return PatchReflection.hasMethod(instance.getClass(), instance, MethodQuery.create().methodName(name).build());
    }

    /**
     * Reflection utility for checking if the patched class has a specific field
     *
     * @param query
     */
    @Override
    public boolean hasField(FieldQuerySpec query) {
        return PatchReflection.hasField(effectiveClass, self, query);
    }

    /**
     * Reflection utility for checking if a specific instance has a specific field
     *
     * @param query
     * @param instance
     */
    @Override
    public boolean hasField(FieldQuerySpec query, Object instance) {
        return PatchReflection.hasField(instance.getClass(), instance, query);
    }

    /**
     * Reflection utility for checking if the patched class has a specific field
     *
     * @param name
     */
    @Override
    public boolean hasField(String name) {
        return PatchReflection.hasField(effectiveClass, self, FieldQuery.create().fieldName(name).build());
    }

    /**
     * Reflection utility for checking if a specific instance has a specific field
     *
     * @param name
     * @param instance
     */
    @Override
    public boolean hasField(String name, Object instance) {
        return PatchReflection.hasField(instance.getClass(), instance, FieldQuery.create().fieldName(name).build());
    }

    /**
     * A transient data store for per-instance data. This data is not stored in the save. It is shared across all patches with access to this instance.
     * Useful for communicating across patches, or if something like a timer is needed. It should use unique keys, not something generic like "target" which multiple mods may use.
     * Throws an IllegalStateException if used on a static method or in @Before on a constructor method, as they have no instance data.
     */
    @Override
    public PatchData getData() {
        return new PatchData(PatchTransientDataStore.getOrCreate(self));
    }
}
