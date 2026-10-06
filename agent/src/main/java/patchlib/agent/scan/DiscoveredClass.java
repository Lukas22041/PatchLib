package patchlib.agent.scan;

import com.fs.starfarer.api.ModSpecAPI;
import net.bytebuddy.description.type.TypeDescription;

/** Internal only, as the type description keeps the whole discovery type pool in memory. */
public record DiscoveredClass(TypeDescription type, ModSpecAPI sourceMod, boolean isFromStarsector) { }
