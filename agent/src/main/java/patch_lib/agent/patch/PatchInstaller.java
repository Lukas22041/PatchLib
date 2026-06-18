package patch_lib.agent.patch;

import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.matcher.ElementMatcher;
import patch_lib.agent.PatchLibLogger;
import patch_lib.agent.matchers.ClassTargetMatcher;
import patch_lib.agent.matchers.GateMatcher;
import patch_lib.agent.matchers.IgnoreMatcher;
import patch_lib.agent.matchers.MethodTargetMatcher;
import patch_lib.agent.spec.PatchSpec;

import java.lang.instrument.Instrumentation;
import java.lang.invoke.MethodHandle;
import java.util.ArrayList;
import java.util.List;

public class PatchInstaller {

    /** A spec with its matchers + handler resolved once, up front. */
    private record InstallData(PatchSpec spec,
                               ElementMatcher<TypeDescription> classMatcher,
                               ElementMatcher<MethodDescription> methodMatcher,
                               MethodHandle handlerMethod) {}

    public static void install(Instrumentation inst, List<PatchSpec> specs, ClassLoader handlerLoader) {
        //Create all matches
        List<InstallData> data = createBundles(specs, handlerLoader);

        // 2) Assemble the agent. Gate = cheap admit; applyToType = authoritative per-method match.
        new AgentBuilder.Default()
                .disableClassFormatChanges()
                .with(AgentBuilder.RedefinitionStrategy.RETRANSFORMATION)
                .with(AgentBuilder.TypeStrategy.Default.DECORATE)
                .with(AgentBuilder.InitializationStrategy.NoOp.INSTANCE)
                .ignore(IgnoreMatcher.create())
                .type(GateMatcher.create(specs))
                .transform((builder, type, loader, module, pd) -> applyToType(builder, type, data))
                .installOn(inst);   // also retransforms already-loaded matching classes
    }

    private static List<InstallData> createBundles(List<PatchSpec> specs, ClassLoader handlerLoader) {
        List<InstallData> data = new ArrayList<>();
        for (PatchSpec spec : specs) {
            MethodHandle handle = resolveHandler(spec, handlerLoader);
            if (handle == null) continue;

            data.add(new InstallData(
                    spec,
                    ClassTargetMatcher.create(spec.targetClass()),
                    MethodTargetMatcher.create(spec.targetMethod()),
                    handle));
        }
        PatchLibLogger.debug("Assembled " + data.size() + " patches");
        return data;
    }

}
