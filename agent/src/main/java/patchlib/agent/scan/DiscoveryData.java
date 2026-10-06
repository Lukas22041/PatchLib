package patchlib.agent.scan;

import net.bytebuddy.dynamic.ClassFileLocator;
import net.bytebuddy.pool.TypePool;
import patchlib.agent.log.PatchLibLogger;

import java.io.IOException;
import java.util.List;

public record DiscoveryData(TypePool pool, List<ClassFileLocator> locators, List<DiscoveredClass> classes) {

    /** Closes the jars that were opened for discovery. */
    public void close() {
        for (ClassFileLocator locator : locators) {
            try {
                locator.close();
            } catch (IOException ex) {
                PatchLibLogger.error("Failed to close a class file locator", ex);
            }
        }
    }

}
