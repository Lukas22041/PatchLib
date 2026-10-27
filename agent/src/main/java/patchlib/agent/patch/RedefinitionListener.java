package patchlib.agent.patch;

import net.bytebuddy.agent.builder.AgentBuilder;
import patchlib.agent.log.PatchLibLogger;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Logs the retransformation of already loaded classes. If the redefinition of already
 * loaded classes fails, it causes bytebuddy to try applying it in seperate patches again
 */
public class RedefinitionListener extends AgentBuilder.RedefinitionStrategy.Listener.Adapter {

    @Override
    public Iterable<? extends List<Class<?>>> onError(int index, List<Class<?>> batch, Throwable throwable, List<Class<?>> types) {
        if (batch.size() > 1) {
            PatchLibLogger.warn("Retransformation of batch " + index + " (" + batch.size() + " classes) failed, retrying it in smaller batches. Cause: " + throwable);
        } else {
            PatchLibLogger.error("Retransformation of " + batch.get(0).getName() + " failed, none of the patches in it are active", throwable);
        }
        //Re-batching is handled by the BatchReallocator
        return Collections.emptyList();
    }

    @Override
    public void onComplete(int amount, List<Class<?>> types, Map<List<Class<?>>, Throwable> failures) {
        long failedClasses = failures.keySet().stream().filter(batch -> batch.size() == 1).count();
        if (failedClasses > 0) {
            PatchLibLogger.error("Retransformed " + types.size() + " already loaded classes in " + amount + " batches, " + failedClasses + " of them could not be patched");
        }
    }
}
