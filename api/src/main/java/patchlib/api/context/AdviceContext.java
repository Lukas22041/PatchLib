package patchlib.api.context;

import patchlib.api.ref.Ref;

/** Shared by the @Before/@After/@Except contexts. These run inside the patched method itself, so its arguments can be
 * replaced. Redirects only observe the host method's arguments, which is why these methods live here and not on Context. */
public interface AdviceContext extends Context {

    /** Writes a new value to an arg */
    void setArg(int index, Object newValue);

    /** Utility for retrieving a typed read/writeable arg of the original called method.
     * Changing an arg in a @Before patch means that the original method will be called and use the modified arguments. */
    <T> Ref<T> getArgRef(int index);
}
