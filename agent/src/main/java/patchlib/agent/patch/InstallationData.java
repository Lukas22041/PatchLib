package patchlib.agent.patch;

import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.matcher.ElementMatcher;
import patchlib.agent.spec.PatchHandlerSpec;

import java.lang.invoke.MethodHandle;

public record InstallationData(
        PatchHandlerSpec spec,
        MethodHandle handler,
        ElementMatcher.Junction<TypeDescription> classMatcher,
        ElementMatcher.Junction<MethodDescription> methodMatcher,
        String errorMessage
) {
}
