package patch_lib.agent.spec;

/** Contains data for resolving a target field based on certain attributes.
 * Types are stored as erased actual names ("" meaning unset) since the scan never loads the candidate classes. */
public record TargetFieldSpec(
        String fieldName,
        String fieldType,
        String fieldSubtype,
        boolean staticOnly
) { }
