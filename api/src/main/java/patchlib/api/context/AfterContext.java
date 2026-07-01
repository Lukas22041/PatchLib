package patchlib.api.context;


public interface AfterContext extends Context {


    /** Retrieves the return value from the original method  */
    Object getReturnValue();
    /** Retrieves the return value from the original method. If multiple patches run on the same method, this can hold another patches return value.
     * Automatically casts the value to the variable/method parameters type that you call it for. */
    <T> T getInferredReturnValue();
    /** Replaces the return value from the original method. */
    void setReturnValue(Object newReturnValue);

    /** Checks if something has skipped the original method in @Before */
    boolean isSkipOriginal();


}
