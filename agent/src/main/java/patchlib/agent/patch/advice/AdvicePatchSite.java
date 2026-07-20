package patchlib.agent.patch.advice;

import patchlib.agent.patch.InstallationData;

import java.util.List;

public class AdvicePatchSite {

    private final List<InstallationData> before;
    private final List<InstallationData> after;
    private final List<InstallationData> except;

    private final String hostClass;
    private final String methodName;
    private final String methodDescriptor;

    public AdvicePatchSite(List<InstallationData> before, List<InstallationData> after, List<InstallationData> except, String hostClass, String methodName, String methodDescriptor) {
        this.before = before;
        this.after = after;
        this.except = except;
        this.hostClass = hostClass;
        this.methodName = methodName;
        this.methodDescriptor = methodDescriptor;
    }


}
