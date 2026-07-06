package patchlib.api.context;


public interface BeforeContext extends HookContext {

    /** Skip the original body and use this as the return value. Does not have an effect on constructors. Use "null" for void methods. */
    void skipOriginal(Object returnValue);

}
