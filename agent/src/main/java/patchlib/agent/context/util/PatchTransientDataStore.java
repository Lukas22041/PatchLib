package patchlib.agent.context.util;

import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class PatchTransientDataStore {

    private static final ReferenceQueue<Object> CLEANUP_QUEUE = new ReferenceQueue<>();
    private static final ConcurrentHashMap<WeakIdentityKey, ConcurrentHashMap<String, Object>> DATA = new ConcurrentHashMap<>();

    private synchronized static Map<String, Object> getOrCreate(Object instance) {
        if (instance == null) {
            throw new IllegalStateException("Can not use Context.getData on a static method or an an @Before patch on a constructor, as no instance to attach to exists");
        }

        purgeGarbageCollectedEntries();

        //Null queue since this key is created just for checking for the instance, it shouldnt register and add extra work to the cleanup queue
        WeakIdentityKey key = new WeakIdentityKey(instance, null);
        ConcurrentHashMap<String, Object> map = DATA.get(key);

        if (map == null) {
            map = new ConcurrentHashMap<>();
            WeakIdentityKey storedKey = new WeakIdentityKey(instance, CLEANUP_QUEUE);
            DATA.put(storedKey, map);
        }

        return map;
    }

    private synchronized static void purgeGarbageCollectedEntries() {
        Object stale = CLEANUP_QUEUE.poll();
        while (stale != null) {
            if (stale instanceof WeakIdentityKey) {
                DATA.remove(stale);
            }
            stale = CLEANUP_QUEUE.poll();
        }
    }

    private static final class WeakIdentityKey extends WeakReference<Object> {
        private final int hash;

        public WeakIdentityKey(Object referent, ReferenceQueue<Object> queue) {
            super(referent, queue);
            this.hash = System.identityHashCode(referent);
        }

        @Override
        public int hashCode() {
            return hash;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (!(obj instanceof WeakIdentityKey)) return false;
            Object stored = get();
            return stored != null && stored == ((WeakIdentityKey) obj).get();
        }
    }

}
