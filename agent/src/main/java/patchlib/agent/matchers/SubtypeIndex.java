package patchlib.agent.matchers;

import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.matcher.ElementMatcher;
import patchlib.agent.data.ClassDataImpl;
import patchlib.agent.log.PatchLibLogger;
import patchlib.agent.patch.InstallationData;
import patchlib.agent.scan.DiscoveryData;
import patchlib.api.data.ClassData;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static net.bytebuddy.matcher.ElementMatchers.failSafe;
import static net.bytebuddy.matcher.ElementMatchers.hasSuperType;

public class SubtypeIndex {

    private Set<String> matches = new HashSet<>();

    public SubtypeIndex(DiscoveryData discoveryData, List<InstallationData> installationDataList) {
        build(discoveryData, installationDataList);
    }

    public boolean contains(String name) {
        return matches.contains(name);
    }

    public void build(DiscoveryData discoveryData, List<InstallationData> installationDataList) {
        long start = System.currentTimeMillis();

        Set<String> targetSubtypes = new HashSet<>();

        for (InstallationData installationData : installationDataList) {
            String targetSubtype = installationData.spec().targetClass().subtypeName();
            if (!targetSubtype.isEmpty()) targetSubtypes.add(targetSubtype);
        }

        ElementMatcher.Junction<TypeDescription> isSubtype = failSafe(hasSuperType(type -> targetSubtypes.contains(type.getActualName())));

        for (ClassData classData : discoveryData.classes()) {
            ClassDataImpl classDataImpl = (ClassDataImpl) classData;
            if (IgnoreMatcher.isIgnored(classDataImpl.getName())) continue;

            TypeDescription typeDescription = classDataImpl.getTypeDescription();

            if (!isSubtype.matches(classDataImpl.getTypeDescription())) continue;

            if (GateMatcher.declaresPatchedMethod(typeDescription, installationDataList)) {
                matches.add(classData.getName());
            }
        }

        long diff = System.currentTimeMillis() - start;
        PatchLibLogger.info("Build subtype index in " + diff + "ms");
    }



}
