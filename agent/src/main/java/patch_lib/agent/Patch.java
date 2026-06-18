package patch_lib.agent;

import patch_lib.agent.spec.PatchSpec;

import java.lang.invoke.MethodHandle;

public record Patch(PatchSpec spec, MethodHandle handler) {
}
