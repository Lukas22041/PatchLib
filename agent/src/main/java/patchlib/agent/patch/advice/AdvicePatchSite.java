package patchlib.agent.patch.advice;

import patchlib.agent.patch.InstallationData;

import java.lang.invoke.MethodHandle;
import java.util.List;

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

    public AdvicePatchSite(MethodHandle beforeChain, MethodHandle afterChain, MethodHandle exceptChain, List<InstallationData> before, List<InstallationData> after, List<InstallationData> except,
                           String hostClass, String methodName, String methodDescriptor) {
        this.beforeChain = beforeChain;
        this.afterChain = afterChain;
        this.exceptChain = exceptChain;
        this.before = before;
        this.after = after;
        this.except = except;
        this.hostClass = hostClass;
        this.methodName = methodName;
        this.methodDescriptor = methodDescriptor;
    }
}
