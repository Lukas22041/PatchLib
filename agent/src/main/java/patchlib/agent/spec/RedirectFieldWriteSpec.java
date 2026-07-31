package patchlib.agent.spec;

import patchlib.api.spec.ClassQuerySpec;
import patchlib.api.spec.FieldQuerySpec;

public record RedirectFieldWriteSpec(ClassQuerySpec owner, FieldQuerySpec field) implements PatchSpec { }
