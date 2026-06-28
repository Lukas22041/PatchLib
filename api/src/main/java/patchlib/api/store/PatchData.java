package patchlib.api.store;

import java.lang.ref.WeakReference;
import java.util.Map;

/** Wrapper around an instance's transient per-instance data map, returned by PatchContext.getData(). */
public final class PatchData {

    private final Map<String, Object> map;

    public PatchData(Map<String, Object> map) {
        this.map = map;
    }

    @SuppressWarnings("unchecked")
    public <T> T get(String key) {
        Object value = map.get(key);
        if (value instanceof Weak weakValue) {
            Object referent = weakValue.get();
            if (referent == null) {
                map.remove(key);
                return null;
            }
            return (T) referent;
        }
        return (T) value;
    }

    public void put(String key, Object value) {
        map.put(key, value);
    }

    /** Stores the value weakly. Only use for values kept alive elsewhere. Prevents memory leaks from things that reference their parent instance, like child UI elements. */
    public void putWeak(String key, Object value) {
        map.put(key, new Weak(value));
    }

    public boolean has(String key) {
        return get(key) != null;
    }

    public void remove(String key) {
        map.remove(key);
    }

    private static final class Weak extends WeakReference<Object> {
        Weak(Object referent) {
            super(referent);
        }
    }
}
