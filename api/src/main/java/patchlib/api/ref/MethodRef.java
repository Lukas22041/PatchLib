package patchlib.api.ref;

import java.lang.invoke.MethodHandle;

/**Wrapper around a method handle that allows enables inferring the return type*/
public final class MethodRef {

    private final MethodHandle handle;

    public MethodRef(MethodHandle handle) { this.handle = handle; }

    public MethodHandle handle() {
        return handle;
    }

    @SuppressWarnings("unchecked")
    public <R> R call(Object... args) {
        return (R) invoke(args);
    }

    private Object invoke(Object... args) {
        try {
            return handle.invokeWithArguments(args);
        } catch (Throwable t) {
            throw new RuntimeException("Failed to invoke method handle", t);
        }
    }
}
