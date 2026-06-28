package patchlib.agent;

import patchlib.agent.spec.PatchSpec;

import java.lang.invoke.MethodHandle;

public record Patch(PatchSpec spec, MethodHandle handler) {
}
