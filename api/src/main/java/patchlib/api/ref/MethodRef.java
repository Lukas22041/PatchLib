package patchlib.api.ref;

import java.lang.invoke.MethodHandle;

/**Wrapper around a method handle that allows inferring the return type and calling it efficiently*/
public final class MethodRef {

    private final MethodHandle handle; //keeps the real method types, receiver unbound
    private final MethodHandle spread; //the same method as an Object[]-in, Object-out handle, adapted once at cache time
    private final Object receiver;     //prepended by call(), null for static methods or when looked up without an instance

    public MethodRef(MethodHandle handle, MethodHandle spread, Object receiver) {
        this.handle = handle;
        this.spread = spread;
        this.receiver = receiver;
    }

    public MethodHandle handle() {
        return receiver == null ? handle : handle.bindTo(receiver);
    }

    @SuppressWarnings("unchecked")
    public <R> R call(Object... args) {
        try {
            Object[] full = receiver == null ? args : prepend(receiver, args);
            return (R) spread.invokeExact(full);
        } catch (Throwable t) {
            throw new RuntimeException("Failed to invoke method handle", t);
        }
    }

    private static Object[] prepend(Object head, Object[] tail) {
        Object[] full = new Object[tail.length + 1];
        full[0] = head;
        System.arraycopy(tail, 0, full, 1, tail.length);
        return full;
    }
}
