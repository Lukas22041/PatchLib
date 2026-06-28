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
) { }
