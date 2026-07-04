package patchlib.agent;

import patchlib.agent.spec.PatchSpec;

import java.lang.invoke.MethodHandle;

/** One discovered patch bound to its handler method, ready to invoke. The blame message is built
 * before the transformer installs: reading the mod spec at dispatch or bootstrap time can call in
 * to patched code and recurse back in to the resolution that needed it. */
public record PatchHandler(PatchSpec spec, MethodHandle handler, String blame) {
}
