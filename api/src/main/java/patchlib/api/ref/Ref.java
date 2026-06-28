package patchlib.api.ref;

/**Mutable reference to a value*/
public interface Ref<T> {
    T get();
    void set(T value);
}
