package patchlib.api;


public interface BeforeContext extends Context {


    /** Skip the original body and use this as the return value. Does not have an effect on constructors. Use "null" for void methods. */
    void skipOriginal(Object returnValue);


}
