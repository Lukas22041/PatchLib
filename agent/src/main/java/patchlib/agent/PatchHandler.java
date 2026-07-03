package patchlib.agent;

import patchlib.agent.spec.PatchSpec;

import java.lang.invoke.MethodHandle;

/** One discovered patch bound to its handler method, ready to invoke. */
public record PatchHandler(PatchSpec spec, MethodHandle handler) {
}
