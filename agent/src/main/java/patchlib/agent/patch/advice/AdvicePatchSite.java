package patchlib.agent.patch.advice;

import patchlib.agent.patch.InstallationData;

import java.util.List;

public record AdvicePatchSite(List<InstallationData> before, List<InstallationData> after, List<InstallationData> except) {



}
