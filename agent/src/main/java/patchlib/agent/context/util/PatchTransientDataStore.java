package patchlib.agent.context.util;

import patchlib.api.store.PatchData;

import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.concurrent.ConcurrentHashMap;

public class PatchTransientDataStore {

    private static final ReferenceQueue<Object> CLEANUP_QUEUE = new ReferenceQueue<>();
    private static final ConcurrentHashMap<Object, PatchData> DATA = new ConcurrentHashMap<>();

    /** Lock free for instances that already have data, only creating new data is synchronised. */
    public static PatchData getOrCreate(Object instance) {
        if (instance == null) {
            throw new IllegalStateException("Can not use Context.getData on a static method or an an @Before patch on a constructor, as no instance to attach to exists");
        }

        //Null queue since this key is created just for checking for the instance, it shouldnt register and add extra work to the cleanup queue
        PatchData data = DATA.get(new WeakIdentityKey(instance, null));
        if (data != null) return data;
        return create(instance);
    }

    private synchronized static PatchData create(Object instance) {
        purgeGarbageCollectedEntries();
        return DATA.computeIfAbsent(new WeakIdentityKey(instance, CLEANUP_QUEUE), key -> new PatchData(new ConcurrentHashMap<>()));
    }

    /** Entries are only added in create, so purging there is enough to keep the map from growing with dead keys. */
    private static void purgeGarbageCollectedEntries() {
        Object stale = CLEANUP_QUEUE.poll();
        while (stale != null) {
            DATA.remove(stale);
            stale = CLEANUP_QUEUE.poll();
        }
    }

    private static final class WeakIdentityKey extends WeakReference<Object> {
        private final int hash;

        WeakIdentityKey(Object referent, ReferenceQueue<Object> queue) {
            super(referent, queue);
            this.hash = System.identityHashCode(referent);
        }

        @Override
        public int hashCode() {
            return hash;
        }

        /** A cleared key only equals itself, which is what lets the purge remove it. */
        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            Object stored = get();
            if (stored == null) return false;
            return obj instanceof WeakIdentityKey key && stored == key.get();
        }
    }

}
