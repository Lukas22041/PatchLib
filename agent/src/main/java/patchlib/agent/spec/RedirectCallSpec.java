package patchlib.agent.spec;

import patchlib.api.spec.ClassQuerySpec;
import patchlib.api.spec.MethodQuerySpec;

public record RedirectCallSpec(ClassQuerySpec owner, MethodQuerySpec call) implements PatchSpec { }
