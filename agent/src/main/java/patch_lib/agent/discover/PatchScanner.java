package patch_lib.agent.discover;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.ModSpecAPI;
import net.bytebuddy.dynamic.ClassFileLocator;
import net.bytebuddy.pool.TypePool;
import patch_lib.agent.PatchLibLogger;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class PatchScanner {

    public void scan() {
        List<ModSpecAPI> enabledMods = Global.getSettings().getModManager().getEnabledModsCopy();
        List<File> jars = enabledMods.stream()
                .flatMap( spec ->
                        spec.getJars().stream()
                                .map( jar -> new File(spec.getPath(), jar)) )
                .toList();

        PatchLibLogger.debug("Starting annotation scan in the following jars: ");
        jars.forEach(jar -> PatchLibLogger.debug(" - " + jar.getPath()));

        List<ClassFileLocator> locators = new ArrayList<>();
        try {
            for (File jar : jars) {
                locators.add(ClassFileLocator.ForJarFile.of(jar));
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        try (ClassFileLocator locator = new ClassFileLocator.Compound(locators);) {

            TypePool pool = new TypePool.Default(new TypePool.CacheProvider.Simple(), locator, TypePool.Default.ReaderMode.FAST);





        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

}
