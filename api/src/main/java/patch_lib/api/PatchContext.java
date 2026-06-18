package patch_lib.api;

public class PatchContext {

    private Object self;
    private final Object[] args;
    private Object returnValue;
    private boolean skipOriginal;

    public PatchContext(Object self, Object[] args) { this.self = self; this.args = args; }

    public Object self() { return self; }
    public void setSelf(Object self) { this.self = self; }   // constructor template only (self isn't available on enter)

    public Object[] args() { return args; }
    public Object getArg(int index) { return args[index]; }
    public void setArg(int index, Object newValue) { args[index] = newValue; }

    public Object getReturnValue() { return returnValue; }
    public void setReturnValue(Object newReturnValue) { this.returnValue = newReturnValue; }

    public boolean isSkipOriginal() { return skipOriginal; }
    /** Skip the original body and use this as the return value. */
    public void skipOriginal(Object returnValue) { this.skipOriginal = true; this.returnValue = returnValue; }

}
