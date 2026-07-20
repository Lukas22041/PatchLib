package patchlib.agent.patch;

import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.utility.JavaModule;
import patchlib.agent.log.PatchLibLogger;

public class InstallListener extends AgentBuilder.Listener.Adapter {

    @Override
    public void onError(String typeName, ClassLoader classLoader, JavaModule module, boolean loaded, Throwable throwable) {
        PatchLibLogger.error("Skipped patching " + typeName + " due to the thrown exception", throwable);
    }
}
