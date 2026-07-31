package patchlib.agent.spec;

import patchlib.api.spec.ClassQuerySpec;
import patchlib.api.spec.MethodQuerySpec;

public record RedirectMethodCallSpec(ClassQuerySpec owner, MethodQuerySpec call) implements PatchSpec { }
