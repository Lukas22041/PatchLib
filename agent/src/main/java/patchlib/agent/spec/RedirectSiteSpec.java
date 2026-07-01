package patchlib.agent.spec;

/** Describes the call site a @Redirect intercepts inside its host method. Owner, name and type are erased actual names
 * ("" meaning unset) since the scan never loads the candidate classes. parameters/parameterCount are unused for field
 * accesses; fieldSubtype is unused for method calls. */
public record RedirectSiteSpec(
        RedirectKind kind,
        String owner,
        String name,
        String[] parameters,
        int parameterCount,
        String returnOrFieldType,
        String fieldSubtype,
        boolean staticOnly
) { }
