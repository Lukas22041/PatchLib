package patchlib.api.context;


public interface BeforeContext extends AdviceContext {

    /** Checks if something has skipped the original method in @Before */
    boolean isSkipOriginal();

    /** Skip the original body and use this as the return value. Does not have an effect on constructors. Use "null" for void methods. */
    void skipOriginal(Object returnValue);


}
