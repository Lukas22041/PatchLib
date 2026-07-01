package patchlib.agent.spec;

/** What a @Redirect intercepts: a method call, a field read, or a field write. */
public enum RedirectKind {
    METHOD_CALL, FIELD_READ, FIELD_WRITE
}
