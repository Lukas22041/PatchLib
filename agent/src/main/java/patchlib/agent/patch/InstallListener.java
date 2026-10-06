package patchlib.agent.patch;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.ModSpecAPI;
import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.utility.JavaModule;
import patchlib.agent.log.PatchLibLogger;

import java.io.File;

public class InstallListener extends AgentBuilder.Listener.Adapter {

    public final static boolean ENABLE_TRANSFORMED_CLASS_DEBUG = false;

    private File outputDir;

    public InstallListener() {
        if (ENABLE_TRANSFORMED_CLASS_DEBUG) {
            outputDir = createTransformedFolder();
        }
    }

    @Override
    public void onError(String typeName, ClassLoader classLoader, JavaModule module, boolean loaded, Throwable throwable) {
        PatchLibLogger.error("Skipped patching " + typeName + " due to the thrown exception", throwable);
    }

    /** Output the transformed classes as .class files to PatchLib/debug/transformed */
    @Override
    public void onTransformation(TypeDescription typeDescription, ClassLoader classLoader, JavaModule module, boolean loaded, DynamicType dynamicType) {
        if (outputDir != null) {
            try {
                dynamicType.saveIn(outputDir);
            } catch (Exception ex) {
                PatchLibLogger.error("Failed to save " + typeDescription.getActualName() + " debug output to " + outputDir.getPath(), ex);
            }
        }
    }


    private File createDebugFolder() {
        ModSpecAPI mod = Global.getSettings().getModManager().getModSpec("patchlib");
        String dir = mod.getPath();

        String outputFolderName = "/debug";
        File debugFolder = new File(dir, outputFolderName);
        if (debugFolder.exists()) {
            debugFolder.delete();
        }
        debugFolder.mkdirs();
        return debugFolder;
    }

    private File createTransformedFolder() {
        File debug = createDebugFolder();

        File transformed = new File(debug, "/transformed");

        if (transformed.exists()) {
            deleteRecursively(transformed);
        }
        transformed.mkdir();
        return transformed;
    }

    private void deleteRecursively(File file) {
        File[] children = file.listFiles();
        if (children != null) {
            for (File child : children) {
                deleteRecursively(child);
            }
        }
        file.delete();
    }
}
