package patchlib.agent.matchers;

import com.fs.starfarer.api.Global;
import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.matcher.ElementMatcher;
import patchlib.agent.spec.PatchSpec;
import patchlib.agent.spec.TargetClassSpec;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static net.bytebuddy.matcher.ElementMatchers.hasSuperType;

public class GateMatcher {

    /** Gates installs to only targets that are mentioned by at least one spec. */
    public static AgentBuilder.RawMatcher create(List<PatchSpec> patches, SubtypeIndex subtypeIndex) {

        //Build a cheap filter, only one of the three is checked, based on if their in the spec or not.
        //Creates a quick filter for simple patches. Only targeting one is fine since its the entry-gate, the exact match is still checked later at install time.
        Set<String> exactNames   = new HashSet<>();
        Set<String> subtypeNames = new HashSet<>();
        List<TargetClassSpec> other = new ArrayList<>();

        for (PatchSpec patch : patches) {
            TargetClassSpec spec = patch.targetClass();
            if (!spec.targetClass().isEmpty()) exactNames.add(spec.targetClass());
            else if (!spec.targetSubtype().isEmpty()) subtypeNames.add(spec.targetSubtype());
            else other.add(spec);
        }

        List<ElementMatcher<TypeDescription>> otherMatchers = new ArrayList<>();
        for (TargetClassSpec spec : other) otherMatchers.add(ClassTargetMatcher.create(spec));

        //Fallback for Janino loaded classes, which don't have a source jar
        ElementMatcher.Junction<TypeDescription> liveSubtype = subtypeNames.isEmpty()
                ? null
                : hasSuperType(matchedType -> subtypeNames.contains(matchedType.getActualName()));

        return (type, classLoader, module, classBeingRedefined, protectionDomain) -> {

            if (!exactNames.isEmpty() && exactNames.contains(type.getActualName())) return true;

            if (!subtypeNames.isEmpty()) {
                if (subtypeIndex.contains(type.getName())) return true;
                //Script classes are never in the jar index; do the (scoped) live check for them instead.
                if (liveSubtype != null && isScriptLoader(classLoader) && liveSubtype.matches(type)) return true;
            }

            for (ElementMatcher<TypeDescription> matcher : otherMatchers) {
                if (matcher.matches(type)) return true;
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

}
