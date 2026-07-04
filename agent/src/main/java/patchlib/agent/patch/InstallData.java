package patchlib.agent.patch;

import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.matcher.ElementMatcher;
import patchlib.agent.spec.PatchSpec;

import java.lang.invoke.MethodHandle;

/** Bundled data for the install process, created once per patch by PatchInstaller.
 * blame is prebuilt here since building it later can call in to patched code, see PatchHandler. */
record InstallData(PatchSpec spec,
                   ElementMatcher<TypeDescription> classMatcher,
                   ElementMatcher<MethodDescription> methodMatcher,
                   MethodHandle handlerMethod,
                   String blame) {}
