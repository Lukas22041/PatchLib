package patchlib.agent;

public record PatchSite(PatchHandler[] beforePatches, PatchHandler[] afterPatches, PatchHandler[] exceptPatches) { }
