package patchlib.agent.patch.advice;

import net.bytebuddy.description.method.MethodDescription;
import patchlib.agent.patch.InstallationData;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodType;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public class AdvicePatchSite {

    public final MethodHandle beforeChain;
    public final MethodHandle afterChain;
    public final MethodHandle exceptChain;

    public final List<InstallationData> before;
    public final List<InstallationData> after;
    public final List<InstallationData> except;

    private final String hostClass;
    private final String methodName;
    private final String methodDescriptor;
    private final boolean isVirtualMethod;

    private final ConcurrentHashMap<Class<?>, Boolean> mostDerivedCache = new ConcurrentHashMap<>();

    public AdvicePatchSite(MethodHandle beforeChain, MethodHandle afterChain, MethodHandle exceptChain, List<InstallationData> before, List<InstallationData> after, List<InstallationData> except,
                           String hostClass, String methodName, String methodDescriptor, boolean isVirtualMethod) {
        this.beforeChain = beforeChain;
        this.afterChain = afterChain;
        this.exceptChain = exceptChain;
        this.before = before;
        this.after = after;
        this.except = except;
        this.hostClass = hostClass;
        this.methodName = methodName;
        this.methodDescriptor = methodDescriptor;
        this.isVirtualMethod = isVirtualMethod;
    }

    public boolean isMostDerived(Class<?> clazz) {
        return mostDerivedCache.computeIfAbsent(clazz, this::computeMostDerived);
    }

    private Boolean computeMostDerived(Class<?> clazz) {

        try {
            //True for constructors if it's the classes own constructor, not that of a superclass
            if (methodName.equals("<init>")) {
                return clazz.getName().equals(hostClass);
            }

            if (!isVirtualMethod) return true;

            Class<?> current = clazz;
            while (current != null) {
                if (declaresMethod(current)) {
                    return current.getName().equals(hostClass);
                }

                current = current.getSuperclass();
            }

            //Missed by both cases above, return true for those cases
            return true;

        } catch (Throwable t) {
            return true;
        }

    }

    private boolean declaresMethod(Class<?> c) {
        for (Method m : c.getDeclaredMethods()) {
            if (m.isBridge() || m.isSynthetic()) continue;
            if (m.getName().equals(methodName)
                    && new MethodDescription.ForLoadedMethod(m).getDescriptor().equals(methodDescriptor)) return true;
        }
        return false;
    }
}
