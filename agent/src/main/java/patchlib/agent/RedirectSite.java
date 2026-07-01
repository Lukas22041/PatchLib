package patchlib.agent;

/** A redirect call site: the priority-ordered layers wrapping one intercepted call. Index 0 is the lowest priority,
 * i.e the outermost layer that runs first; its call() reaches the next layer, the innermost reaches the real call. */
public record RedirectSite(Patch[] layers) { }
