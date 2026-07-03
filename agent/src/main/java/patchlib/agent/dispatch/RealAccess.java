package patchlib.agent.dispatch;

/** The original access at the bottom of a redirect chain, adapted once per site.
 * target is the call receiver or field owner, ignored by receiverless accesses. */
@FunctionalInterface
public interface RealAccess {
    Object call(Object target, Object[] args) throws Throwable;
}
