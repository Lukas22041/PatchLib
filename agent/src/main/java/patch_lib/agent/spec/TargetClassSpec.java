package patch_lib.agent.spec;

/** Contains data for resolving a target class based on certain attributes */
public record TargetClassSpec(
        String targetClass,
        String targetSubtype,
        String targetPackage,
        boolean includeSubpackages,
        TargetMethodSpec[] methodMatches,
        TargetFieldSpec[] fieldMatches
) { }
