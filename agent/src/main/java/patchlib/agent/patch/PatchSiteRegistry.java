package patchlib.agent.patch;

import patchlib.agent.spec.PatchHandlerSpec;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Tracks every patch handler and the methods it was installed on. Sites are added as their classes load. */
public class PatchSiteRegistry {

    public record Site(String className, String methodName, String descriptor) { }

    private static final Map<PatchHandlerSpec, Set<Site>> SITES = new LinkedHashMap<>();

    public static synchronized void registerHandlers(List<InstallationData> installationDataList) {
        for (InstallationData data : installationDataList) {
            SITES.computeIfAbsent(data.spec(), key -> new LinkedHashSet<>());
        }
    }

    public static synchronized void addSite(List<InstallationData> installationDataList, String className, String methodName, String descriptor) {
        Site site = new Site(className, methodName, descriptor);
        for (InstallationData data : installationDataList) {
            SITES.computeIfAbsent(data.spec(), key -> new LinkedHashSet<>()).add(site);
        }
    }

    public static synchronized List<PatchHandlerSpec> getHandlers() {
        return new ArrayList<>(SITES.keySet());
    }

    public static synchronized List<Site> getSites(PatchHandlerSpec handler) {
        return new ArrayList<>(SITES.getOrDefault(handler, Set.of()));
    }

}
