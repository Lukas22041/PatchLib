package patch_lib.agent.matchers;

import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.matcher.ElementMatcher;
import patch_lib.agent.spec.PatchSpec;
import patch_lib.agent.spec.TargetClassSpec;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static net.bytebuddy.matcher.ElementMatchers.none;

public class GateMatcher {

    /** Gates installs to only targets that are mentioned by atleast one spec*/
    public static ElementMatcher.Junction<TypeDescription> create(List<PatchSpec> patches, SubtypeIndex subtypeIndex) {

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

        ElementMatcher.Junction<TypeDescription> gate = none();

        if (!exactNames.isEmpty())
            gate = gate.or((TypeDescription t) -> exactNames.contains(t.getActualName()));

        if (!subtypeNames.isEmpty())
            gate = gate.or((TypeDescription type) -> subtypeIndex.contains(type.getName()));

        for (TargetClassSpec spec : other)
            gate = gate.or(ClassTargetMatcher.create(spec));

        return gate;
    }

}
