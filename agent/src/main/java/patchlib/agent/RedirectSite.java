package patchlib.agent;

import patchlib.agent.dispatch.RealAccess;
import patchlib.agent.spec.RedirectKind;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodType;

/** A redirect call site: the priority-ordered layers wrapping one intercepted call. Index 0 is the lowest priority,
 * i.e the outermost layer that runs first; its call() reaches the next layer, the innermost reaches the real call. */
public final class RedirectSite {

    private final PatchHandler[] layers;
    private final RedirectKind kind;

    /** The original access at the chain bottom, adapted once on first dispatch, see realAccess(). */
    private volatile RealAccess realAccess;

    public RedirectSite(PatchHandler[] layers, RedirectKind kind) {
        this.layers = layers;
        this.kind = kind;
    }

    public PatchHandler[] layers() {
        return layers;
    }

    /** The adapted original access, created on first dispatch. Every dispatch of a site carries the same underlying
     * member, so the adaptation is reusable; the race on first dispatch is benign, both threads build an equivalent access. */
    public RealAccess realAccess(MethodHandle original, int argCount) {
        RealAccess access = realAccess;
        if (access == null) {
            access = createRealAccess(original, original.type().parameterCount() > argCount);
            realAccess = access;
        }
        return access;
    }

    /** Adapts the original handle to a generic form invoked via invokeExact, which skips the per-call type checking
     * and boxing setup that invokeWithArguments redoes on every access. The receiver stays a leading positional
     * parameter, so no argument array is rebuilt per call. */
    private RealAccess createRealAccess(MethodHandle original, boolean hasReceiver) {
        int count = original.type().parameterCount();
        MethodHandle generic = original.asType(MethodType.genericMethodType(count));
        return switch (kind) {
            case METHOD_CALL, CONSTRUCTOR -> {
                if (!hasReceiver) { //also every constructor: the handle allocates and initializes in one step
                    MethodHandle spread = generic.asSpreader(Object[].class, count);
                    yield (target, args) -> (Object) spread.invokeExact(args);
                }
                MethodHandle spread = generic.asSpreader(Object[].class, count - 1);
                yield (target, args) -> (Object) spread.invokeExact(target, args);
            }
            case FIELD_READ -> hasReceiver
                    ? (target, args) -> (Object) generic.invokeExact(target)
                    : (target, args) -> (Object) generic.invokeExact();
            //A write has no result, the adapted handle returns the null that asType makes of the void return.
            case FIELD_WRITE -> hasReceiver
                    ? (target, args) -> (Object) generic.invokeExact(target, args[0])
                    : (target, args) -> (Object) generic.invokeExact(args[0]);
        };
    }
}
