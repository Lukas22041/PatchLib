package patchlib.agent.context;

import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** Backing store for per-instance patch data, keyed weakly by instance identity. Internal to the context impls;
 * mods reach it only through BaseContext.getData(), which wraps the returned map in a PatchData. */
final class PatchStore {

    private PatchStore() {}

    private static final ReferenceQueue<Object> QUEUE = new ReferenceQueue<>();
    private static final ConcurrentHashMap<Key, ConcurrentHashMap<String, Object>> DATA = new ConcurrentHashMap<>();

    /** The data map for an instance, created on first use. */
    static Map<String, Object> getOrCreate(Object instance) {
        if (instance == null) {
            throw new IllegalStateException("Can not use PatchContext.getData on a static method or on a @Before constructor method, as those have no instance.");
        }

        purge();
        ConcurrentHashMap<String, Object> map = DATA.get(new Key(instance, null));
        if (map != null) return map;
        ConcurrentHashMap<String, Object> created = new ConcurrentHashMap<>();
        //Concurrency safety check, in case something else has also just deposited a new map.
        ConcurrentHashMap<String, Object> prev = DATA.putIfAbsent(new Key(instance, QUEUE), created);
        return prev != null ? prev : created;
    }

    /** Remove entries whose key object has been garbage-collected. */
    private static void purge() {
        Object stale;
        while ((stale = QUEUE.poll()) != null) {
            DATA.remove(stale);
        }
    }

    /** Weak reference that hashes/compares by identity. */
    private static final class Key extends WeakReference<Object> {
        private final int hash;

        Key(Object referent, ReferenceQueue<Object> queue) {
            super(referent, queue);
            this.hash = System.identityHashCode(referent);
        }

        @Override
        public int hashCode() {
            return hash;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Key)) return false;
            Object self = get();
            return self != null && self == ((Key) o).get();
        }
    }
}
