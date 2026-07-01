package patchlib.agent;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodType;

/** A redirect call site: the priority-ordered layers wrapping one intercepted call. Index 0 is the lowest priority,
 * i.e the outermost layer that runs first; its call() reaches the next layer, the innermost reaches the real call. */
public final class RedirectSite {

    private final Patch[] layers;

    /** The original access as an Object[]-in, Object-out handle, adapted once on first dispatch. Invoking it via
     * invokeExact skips the per-call type checking and boxing setup that invokeWithArguments redoes on every access. */
    private volatile MethodHandle spreadOriginal;

    public RedirectSite(Patch[] layers) {
        this.layers = layers;
    }

    public Patch[] layers() {
        return layers;
    }

    /** The adapted form of the original access. Every dispatch of a site carries the same underlying member, so the
     * adaptation is reusable; the race on first dispatch is benign, both threads adapt to an equivalent handle. */
    public MethodHandle spreadOriginal(MethodHandle original) {
        MethodHandle handle = spreadOriginal;
        if (handle == null) {
            int parameterCount = original.type().parameterCount();
            handle = original.asType(MethodType.genericMethodType(parameterCount)).asSpreader(Object[].class, parameterCount);
            spreadOriginal = handle;
        }
        return handle;
    }
}
