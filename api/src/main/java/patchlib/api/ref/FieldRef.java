package patchlib.api.ref;

import java.lang.reflect.Field;

/**A mutable reference to a field
 * Loaded from the System class loader on the agent, so can use reflection without issue. */
public final class FieldRef<T> implements Ref<T> {

    private final Field field;
    private final Object target;

    public FieldRef(Field field, Object target) {
        this.field = field;
        this.target = target;
    }

    @Override
    @SuppressWarnings("unchecked")
    public T get() {
        try {
            return (T) field.get(target);
        } catch (IllegalAccessException e) {
            throw new RuntimeException("Could not read field " + field, e);
        }
    }

    @Override
    public void set(T value) {
        try {
            field.set(target, value);
        } catch (IllegalAccessException e) {
            throw new RuntimeException("Could not set field " + field, e);
        }
    }
}
