package patchlib.agent;

import net.bytebuddy.description.method.MethodDescription;

import java.lang.reflect.Method;

/** Represents one patched method. The method identity is used for enabling the isMostDerived method, which tells a handler whether it runs
 * on the body a virtual call would resolve to. */
public final class PatchSite {

    private final PatchHandler[] beforePatches;
    private final PatchHandler[] afterPatches;
    private final PatchHandler[] exceptPatches;

    //Identity of the woven method; methodName is "<init>" for constructors.
    private final String hostClass;
    private final String methodName;
    private final String methodDescriptor;

    /** Cached per runtime class: does dispatch of this method resolve to hostClass. */
    private final ClassValue<Boolean> mostDerived = new ClassValue<>() {
        @Override
        protected Boolean computeValue(Class<?> runtime) {
            return computeMostDerived(runtime);
        }
    };

    public PatchSite(PatchHandler[] beforePatches, PatchHandler[] afterPatches, PatchHandler[] exceptPatches,
                     String hostClass, String methodName, String methodDescriptor) {
        this.beforePatches = beforePatches;
        this.afterPatches = afterPatches;
        this.exceptPatches = exceptPatches;
        this.hostClass = hostClass;
        this.methodName = methodName;
        this.methodDescriptor = methodDescriptor;
    }

    public PatchHandler[] beforePatches() { return beforePatches; }
    public PatchHandler[] afterPatches() { return afterPatches; }
    public PatchHandler[] exceptPatches() { return exceptPatches; }

    /** True if hostClass is the implementation a normal call on a runtime instance resolves to, false if this
     * body was only reached through a subclass super call. */
    public boolean isMostDerived(Class<?> runtime) {
        return mostDerived.get(runtime);
    }

    private boolean computeMostDerived(Class<?> runtime) {
        try {
            //Constructors are not virtual: the effective frame is the runtime class's own constructor.
            if (methodName.equals("<init>")) {
                return runtime.getName().equals(hostClass);
            }
            //First declarer walking up from the runtime class is where dispatch lands. The host always
            //declares the method, so this matches at or before it.
            for (Class<?> c = runtime; c != null; c = c.getSuperclass()) {
                if (declaresMethod(c)) {
                    return c.getName().equals(hostClass);
                }
            }
            //Not on the superclass chain, e.g. an interface default method. Do not suppress.
            return true;
        } catch (Throwable t) {
            //Never break dispatch over this query; fall back to firing.
            return true;
        }
    }

    private boolean declaresMethod(Class<?> c) {
        for (Method m : c.getDeclaredMethods()) {
            if (m.isBridge() || m.isSynthetic()) continue;
            //ByteBuddy descriptor so it matches how the site's was produced at install.
            if (m.getName().equals(methodName)
                    && new MethodDescription.ForLoadedMethod(m).getDescriptor().equals(methodDescriptor)) return true;
        }
        return false;
    }
}
