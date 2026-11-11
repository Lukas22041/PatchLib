package patchlib.agent.patch.redirect;

import patchlib.agent.patch.InstallationData;
import patchlib.agent.spec.PatchHandlerSpec;

import java.lang.constant.DirectMethodHandleDesc;
import java.util.List;

/**
 * @param hostLoader the classloader of the class that contains the redirected code
 * @param original describes the method call, constructor call or field access that the redirect replaced. It is only turned in to a handle once the redirected code runs for the first time
 */
public record RedirectPatchSite(List<InstallationData> installationDataList, PatchHandlerSpec.RedirectType redirectType,
                                ClassLoader hostLoader, String hostClassName, DirectMethodHandleDesc original, boolean hasReceiver) { }
