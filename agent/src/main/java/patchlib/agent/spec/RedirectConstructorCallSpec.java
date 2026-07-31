package patchlib.agent.spec;

import patchlib.api.spec.ClassQuerySpec;
import patchlib.api.spec.MethodQuerySpec;

public record RedirectConstructorCallSpec(ClassQuerySpec constructed, MethodQuerySpec constructor) implements PatchSpec { }
