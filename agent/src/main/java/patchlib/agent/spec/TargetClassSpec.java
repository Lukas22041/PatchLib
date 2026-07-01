package patchlib.agent.spec;

/** Contains data for resolving a target class based on certain attributes */
public record TargetClassSpec(
        String targetClass,
        String targetSubtype,
        String targetPackage,
        boolean includeSubpackages,
        String excludePackage,
        boolean excludeSubpackages,
        TargetMethodSpec[] methodMatches,
        TargetFieldSpec[] fieldMatches
) {

    /** True when no filter is set, i.e the spec matches every class. */
    public boolean matchesEverything() {
        return targetClass.isEmpty() && targetSubtype.isEmpty() && targetPackage.isEmpty()
                && excludePackage.isEmpty() && methodMatches.length == 0 && fieldMatches.length == 0;
    }
}
