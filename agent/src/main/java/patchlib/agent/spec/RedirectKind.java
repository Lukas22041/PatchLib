package patchlib.agent.spec;

/** What a redirect intercepts: a method call, a field read, or a field write. One kind per redirect annotation. */
public enum RedirectKind {
    METHOD_CALL, FIELD_READ, FIELD_WRITE
}
