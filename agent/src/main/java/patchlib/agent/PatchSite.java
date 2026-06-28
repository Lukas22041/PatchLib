package patchlib.agent;

public record PatchSite(Patch[] beforePatches, Patch[] afterPatches, Patch[] exceptPatches) { }
