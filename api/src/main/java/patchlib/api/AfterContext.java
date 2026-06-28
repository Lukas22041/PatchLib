package patchlib.api;

import patchlib.api.query.FieldQuery;
import patchlib.api.query.MethodQuery;
import patchlib.api.ref.MethodRef;
import patchlib.api.ref.Ref;
import patchlib.api.store.PatchData;

public interface AfterContext {

    /** Get the instance that the patched method is called on. Can be null on @Before constructors and on all static methods */
    Object getSelf();
    /** Get the instance that the patched method is called on. Can be null on @Before constructors and on all static methods
     * Automatically casts the value to the variable/method parameters type that you call it for. */
    <T> T getInferredSelf();

    /**Gets the passed in arguments. Writing to a spot in the array will replace the original value */
    Object[] getArgs();
    /**Gets a read-only argument from the passed in arguments of the method.
     * While you can not replace the instance itself with this, you can still modify the member variables of the object. */
    Object getArg(int index);

    /** Retrieves the return value from the original method  */
    Object getReturnValue();
    /** Retrieves the return value from the original method. If multiple patches run on the same method, this can hold another patches return value.
     * Automatically casts the value to the variable/method parameters type that you call it for. */
    <T> T getInferredReturnValue();
    /** Replaces the return value from the original method. */
    void setReturnValue(Object newReturnValue);

    /** Utility for retrieving a typed read/writeable arg of the original called method.
     * Changing an arg in a @Before patch means that the original method will be called and use the modified arguments. */
    <T> Ref<T> getArgRef(int index);

    /** Reflection utility for reading/writing a typed field from the instance. Most useful for private members of a class, since reflection is otherwise blocked. First match wins. */
    <T> Ref<T> getField(FieldQuery query);

    /** Reflection utility for reading/writing a typed field from the given object. Most useful for private members of a class, since reflection is otherwise blocked. First match wins. */
    <T> Ref<T> getField(Object instance, FieldQuery query);

    /** Reflection utility for receiving a method from the instance. Most useful for private members of a class, since reflection is otherwise blocked. First match wins. */
    MethodRef getMethod(MethodQuery query);

    /** Reflection utility for receiving a method from the given object. Most useful for private members of a class, since reflection is otherwise blocked. First match wins. */
    MethodRef getMethod(Object instance, MethodQuery query);

    /** Reflection utility for checking if the patched class has a specific method */
    boolean hasMethod(MethodQuery query);

    /** Reflection utility for checking if a specific instance has a specific method */
    boolean hasMethod(Object instance, MethodQuery query);

    /** Reflection utility for checking if the patched class has a specific field */
    boolean hasField(FieldQuery query);

    /** Reflection utility for checking if a specific instance has a specific field */
    boolean hasField(Object instance, FieldQuery query);

    /**A transient data store for per-instance data This data is not stored in the save. It is shared across all patches with access to this instance.
     * Useful for communicating across patches, or if something like a timer is needed. It should use unique keys, not something generic like "target" which multiple mods may use.
     * Throws an IllegalStateException if used on a static method or in @Before on a constructor method, as they have no instance data.  */
    PatchData getData();
}
