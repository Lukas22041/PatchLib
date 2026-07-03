package patchlib.agent.patch;

import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.matcher.ElementMatcher;
import patchlib.agent.spec.PatchSpec;

import java.lang.invoke.MethodHandle;

/** Bundled data for the install process, created once per patch by PatchInstaller. */
record InstallData(PatchSpec spec,
                   ElementMatcher<TypeDescription> classMatcher,
                   ElementMatcher<MethodDescription> methodMatcher,
                   MethodHandle handlerMethod) {}
