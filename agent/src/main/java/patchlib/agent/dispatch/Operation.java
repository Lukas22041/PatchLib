package patchlib.agent.dispatch;

/** One step in a redirect chain: a layer's handler, or at the bottom the real call. The args exclude the call's
 * receiver; the innermost step adds it back when invoking the original. */
@FunctionalInterface
public interface Operation {
    Object call(Object[] args) throws Throwable;
}
