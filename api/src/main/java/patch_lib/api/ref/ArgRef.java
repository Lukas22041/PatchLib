package patch_lib.api.ref;

/**A mutable reference to a method parameter */
public final class ArgRef<T> implements Ref<T> {

    private final Object[] args;
    private final int index;

    public ArgRef(Object[] args, int index) {
        this.args = args;
        this.index = index;
    }

    @Override
    @SuppressWarnings("unchecked")
    public T get() { return (T) args[index]; }

    @Override
    public void set(T value) { args[index] = value; }
}
