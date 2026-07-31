package patchlib.agent.spec;

import patchlib.api.spec.ClassQuerySpec;
import patchlib.api.spec.FieldQuerySpec;

public record RedirectFieldReadSpec(ClassQuerySpec owner, FieldQuerySpec field) implements PatchSpec { }
