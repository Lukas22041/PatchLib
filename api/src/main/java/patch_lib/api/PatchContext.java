package patch_lib.api;

import patch_lib.api.query.FieldQuery;
import patch_lib.api.query.MethodQuery;
import patch_lib.api.ref.MethodRef;
import patch_lib.api.ref.Ref;

public class PatchContext {

    private Class<?> owner;
    private Object self;
    private final Object[] args;
    private Object returnValue;
    private boolean skipOriginal;

    public PatchContext(Class<?> owner, Object self, Object[] args) {
        this.owner = owner;
        this.self = self;
        this.args = args;
    }

    public Object self() { return self; }
    public void setSelf(Object self) { this.self = self; }   // constructor template only (self isn't available on enter)

    public Object[] args() { return args; }
    public Object getArg(int index) { return args[index]; }
    public void setArg(int index, Object newValue) { args[index] = newValue; }

    public Object getReturnValue() { return returnValue; }
    public void setReturnValue(Object newReturnValue) { this.returnValue = newReturnValue; }

    public boolean isSkipOriginal() { return skipOriginal; }
    /** Skip the original body and use this as the return value. Does not have an effect on constructors*/
    public void skipOriginal(Object returnValue) { this.skipOriginal = true; this.returnValue = returnValue; }

    /** Utility for retrieving a typed read/writeable arg of the original called method.
     * Changing an arg in a @Before patch means that the original method will be called and use the modified arguments. */
    public <T> Ref<T> getArgRef(int index) { return ClassMembers.arg(args, index); }

    /** Reflection utility for reading/writing a typed field from the instance. Most useful for private members of a class, since reflection is otherwise blocked. First match wins. */
    public <T> Ref<T> getField(FieldQuery query) { return ClassMembers.field(owner, self, query); }

    /** Reflection utility for receiving a method from the instance. Most useful for private members of a class, since reflection is otherwise blocked. First match wins. */
    public MethodRef getMethod(MethodQuery query) { return ClassMembers.method(owner, self, query); }
}
