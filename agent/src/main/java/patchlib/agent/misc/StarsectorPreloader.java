package patchlib.agent.misc;

import patchlib.agent.data.ClassDataImpl;
import patchlib.agent.log.PatchLibLogger;
import patchlib.agent.scan.DiscoveryData;
import patchlib.api.data.ClassData;

public class StarsectorPreloader {

    private DiscoveryData data;
    private ClassLoader classLoader;

    public StarsectorPreloader(DiscoveryData data, ClassLoader classLoader) {
        this.data = data;
        this.classLoader = classLoader;
    }

    public void preload() {
        PatchLibLogger.info("Starting starsector class preload");
        long start = System.currentTimeMillis();

        int loaded = 0;
        int skipped = 0;
        for (ClassData classData : data.classes()) {
            //Skip modded classes for now, only preload starsectors own classes
            //If ever changed, the passed in class loader also needs to be changed to the mod class loader.
            if (!((ClassDataImpl) classData).isFromStarsector()) continue;
            //if (classData.getSourceMod() != null) continue;

            try {
                //Load with "initialize" set to false prevents static blocks from being called early.
                Class.forName(classData.getName(), false, classLoader);
                loaded++;
            } catch (Exception ex) {
                skipped++;
            }

        }

        float diff = System.currentTimeMillis() - start ;
        PatchLibLogger.info("Loaded " + loaded + " starsector classes during preload");
        PatchLibLogger.info("Skipped " + skipped + " starsector classes during preload");
        PatchLibLogger.info("Finished starsector class preload in " + diff + " ms");
        PatchLibLogger.blank();
    }
}
