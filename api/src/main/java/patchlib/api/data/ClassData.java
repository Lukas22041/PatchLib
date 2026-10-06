package patchlib.api.data;

import com.fs.starfarer.api.ModSpecAPI;

/** A class found by PatchLib.scan(). The source mod is null for classes from the games own jars. */
public record ClassData(Class<?> type, ModSpecAPI sourceMod, boolean isFromStarsector) { }
