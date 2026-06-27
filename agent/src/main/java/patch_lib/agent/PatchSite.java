package patch_lib.agent;

public record PatchSite(Patch[] beforePatches, Patch[] afterPatches, Patch[] exceptPatches) { }
