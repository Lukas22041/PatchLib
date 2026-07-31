package patchlib.agent.patch.redirect;

import patchlib.agent.patch.InstallationData;
import patchlib.agent.spec.PatchHandlerSpec;

import java.util.List;

public record RedirectPatchSite(List<InstallationData> installationDataList, PatchHandlerSpec.RedirectType redirectType) { }
