package patchlib.agent.matchers;

import com.fs.starfarer.api.Global;
import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.matcher.ElementMatcher;
import patchlib.agent.patch.InstallationData;
import patchlib.agent.spec.PatchHandlerSpec;
import patchlib.agent.spec.PatchSpec;
import patchlib.api.spec.ClassQuerySpec;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static net.bytebuddy.matcher.ElementMatchers.*;

public class GateMatcher {

    /** Gates installs to only targets that are mentioned by at least one spec. */
    public static AgentBuilder.RawMatcher create(List<InstallationData> installationDataList, SubtypeIndex subtypeIndex) {

        //Build a cheap filter, each patch lands in exactly one of the three buckets below.
        //Only checking one filter per patch is fine since this is the entry-gate, the exact match is still checked later at install time.
        Set<String> exactNames   = new HashSet<>();
        Set<String> subtypeNames = new HashSet<>();
        List<ClassQuerySpec> other = new ArrayList<>();

        for (InstallationData installationData : installationDataList) {
            ClassQuerySpec spec = installationData.spec().targetClass();
            if (!spec.className().isEmpty()) exactNames.add(spec.className());
            else if (!spec.subtypeName().isEmpty()) subtypeNames.add(spec.subtypeName());
            else other.add(spec);
        }

        List<ElementMatcher<TypeDescription>> otherMatchers = new ArrayList<>();
        for (ClassQuerySpec spec : other) otherMatchers.add(ClassMatcher.fromQuery(spec));

        //Fallback for Janino loaded classes, which don't have a source jar
        ElementMatcher.Junction<TypeDescription> liveSubtype =
                subtypeNames.isEmpty() ? null : hasSuperType(matchedType -> subtypeNames.contains(matchedType.getActualName()));

        return (type, classLoader, module, classBeingRedefined, protectionDomain) -> {

            if (!exactNames.isEmpty() && exactNames.contains(type.getActualName()) && declaresPatchedMethod(type, installationDataList)) return true;

            if (!subtypeNames.isEmpty()) {
                if (subtypeIndex.contains(type.getName())) return true;
                //Script classes are never in the jar index; do the (scoped) live check for them instead.
                if (liveSubtype != null && isScriptLoader(classLoader) && liveSubtype.matches(type) && declaresPatchedMethod(type, installationDataList)) return true;
            }

            for (ElementMatcher<TypeDescription> matcher : otherMatchers) {
                if (matcher.matches(type) && declaresPatchedMethod(type, installationDataList)) return true;
            }
            return false;
        };
    }


    private static boolean isScriptLoader(ClassLoader loader) {
        if (loader == null) return false;
        try {
            return loader == Global.getSettings().getScriptClassLoader();
        } catch (Throwable t) {
            //Settings/script loader not initialised yet (very early class loads), no loose scripts exist at that point.
            return false;
        }
    }

    public static boolean declaresPatchedMethod(TypeDescription type, List<InstallationData> installationDataList) {
        try {
            for (InstallationData data : installationDataList) {
                if (!data.classMatcher().matches(type)) continue;
                for (MethodDescription.InDefinedShape method : type.getDeclaredMethods()) {
                    if (method.isAbstract() || method.isNative()) continue;
                    if (data.methodMatcher().matches(method)) return true;
                }
            }
            return false;
        } catch (Throwable t) {
            return true;
        }
    }

}
