package patchlib.agent.spec;

/** Describes the call site a redirect intercepts inside its host method. Name and type are erased actual names
 * ("" meaning unset) since the scan never loads the candidate classes. The owner is the class declaring the
 * intercepted member, built from the annotation's @ClassMatch. parameters/parameterCount are unused for field
 * accesses; fieldSubtype is unused for method calls. */
public record RedirectSiteSpec(
        RedirectKind kind,
        TargetClassSpec owner,
        String name,
        String[] parameters,
        int parameterCount,
        String returnOrFieldType,
        String fieldSubtype,
        boolean staticOnly
) { }
