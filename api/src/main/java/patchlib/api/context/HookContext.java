package patchlib.api.context;

import patchlib.api.ref.Ref;

/** Shared by the @Before/@After/@Except contexts. These run inside the patched method itself, so its arguments can be
 * replaced.  */
public interface HookContext extends Context {

    /** Writes a new value to an arg */
    void setArg(int index, Object newValue);

    /** Utility for retrieving a typed read/writeable arg of the original called method.
     * Changing an arg in a @Before patch means that the original method will be called and use the modified arguments. */
    <T> Ref<T> getArgRef(int index);

    /** Checks if something has skipped the original method in @Before */
    boolean isSkipOriginal();

    /** Stores a value for the duration of this single method call, shared with all before/after/except patches. */
    void setLocal(String key, Object value);

    /** Retrieves a value stored by setLocal during this method call. shared with all before/after/except patches. */
    <T> T getLocal(String key);
}
