package patchlib.agent;

import patchlib.agent.spec.RedirectKind;

/** A redirect call site: the priority-ordered layers wrapping one intercepted call, and its kind.
 * Index 0 is the lowest priority, i.e the outermost layer that runs first. ChainBootstrap composes
 * the runnable chain from this data when the host class resolves its site constant. */
public record RedirectSite(PatchHandler[] layers, RedirectKind kind) { }
