package patch_lib.agent.patch;

import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.matcher.ElementMatcher;
import net.bytebuddy.matcher.ElementMatchers;
import net.bytebuddy.utility.JavaModule;
import patch_lib.agent.Patch;
import patch_lib.agent.PatchLibLogger;
import patch_lib.agent.PatchRegistry;
import patch_lib.agent.PatchSite;
import patch_lib.agent.dispatch.DispatchIdMarker;
import patch_lib.agent.matchers.ClassTargetMatcher;
import patch_lib.agent.matchers.GateMatcher;
import patch_lib.agent.matchers.IgnoreMatcher;
import patch_lib.agent.matchers.MethodTargetMatcher;
import patch_lib.agent.patch.template.ConstructorTemplate;
import patch_lib.agent.patch.template.ReturnTemplate;
import patch_lib.agent.patch.template.VoidTemplate;
import patch_lib.agent.spec.PatchSpec;
import patch_lib.agent.spec.PatchType;
import patch_lib.api.PatchContext;

import java.lang.instrument.Instrumentation;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class PatchInstaller {

    /** Bundled data for the install process */
    private record InstallData(PatchSpec spec,
                               ElementMatcher<TypeDescription> classMatcher,
                               ElementMatcher<MethodDescription> methodMatcher,
                               MethodHandle handlerMethod) {}

    public static void install(Instrumentation inst, List<PatchSpec> specs, ClassLoader handlerLoader) {
        //Create all the data that will be used for the install process once
        List<InstallData> data = setupData(specs, handlerLoader);

        new AgentBuilder.Default()
                .disableClassFormatChanges()
                .with(AgentBuilder.RedefinitionStrategy.RETRANSFORMATION) //Enables transforming existing classes & registers itself for future re-transformations
                .with(AgentBuilder.TypeStrategy.Default.DECORATE) //Prevents bytebuddy from making changes to the classes shape, which would be incompatible with retransformation
                .with(AgentBuilder.InitializationStrategy.NoOp.INSTANCE)
                .with(new AgentBuilder.Listener.Adapter() {
                    @Override
                    public void onError(String typeName, ClassLoader classLoader, JavaModule module, boolean loaded, Throwable throwable) {
                        PatchLibLogger.error("Skipping patching " + typeName + " due to the thrown exception: " + throwable);
                    }
                })
                .ignore(IgnoreMatcher.create()) //Ignore attempts to patch core JVM, bytebuddy and PatchLib methods.
                .type(GateMatcher.create(specs)) //Filter out classes that aren't relevant to any patch

                //Transform is called in three scenarios for classes that get through the two filters above
                // - Once for every already loaded class on install
                // - Once every time a new class is loaded after install
                // - Every time a class is re-transformed by another source.
                .transform((builder, type, loader, module, pd) -> transform(builder, type, data))
                .installOn(inst);
    }

    /** Applies the bytecode change per method once
     * Two or more patches on the same target method still only do one modification of the bytecode
     * Instead of directly inserting a call to the handler, it inserts a call to PatchDispatchers enter and exit methods. */
    private static DynamicType.Builder<?> transform(DynamicType.Builder<?> builder, TypeDescription type, List<InstallData> dataList) {

        List<InstallData> forType = new ArrayList<>();
        for (InstallData data : dataList) {
            if (data.classMatcher.matches(type)) {
                forType.add(data);
            }
        }

        if (forType.isEmpty()) return builder; //Shouldn't happen due to the gate, but just in case.

        for (MethodDescription.InDefinedShape method : type.getDeclaredMethods()) {

            if (method.isAbstract() || method.isNative()) continue; //Methods with no body to install in to, skip.

            List<InstallData> matches = new ArrayList<>();
            for (InstallData data : forType) {
                if (data.methodMatcher.matches(method)) {
                    matches.add(data);
                }
            }

            if (matches.isEmpty()) continue;

            String key = type.getName() + "#" + method.getInternalName() + method.getDescriptor();
            int id = PatchRegistry.register(key, createPatchSite(matches));

            builder = builder.visit(
                    Advice.withCustomMapping()
                            .bind(DispatchIdMarker.class, id) //Attach the Dispatch ID
                            .to(pickTemplate(method)) //Pick the right template per method type
                            .on(ElementMatchers.is(method))
            );

            PatchLibLogger.info("Installed a patch site at " + type.getActualName() + " for method " + method.getActualName());
        }
        return builder;
    }

    private static List<InstallData> setupData(List<PatchSpec> specs, ClassLoader handlerLoader) {
        List<InstallData> data = new ArrayList<>();
        for (PatchSpec spec : specs) {
            MethodHandle handle = createMethodHandle(spec, handlerLoader);
            if (handle == null) continue;

            data.add(new InstallData(
                    spec,
                    ClassTargetMatcher.create(spec.targetClass()),
                    MethodTargetMatcher.create(spec.targetMethod()),
                    handle));
        }
        PatchLibLogger.info("Assembled " + data.size() + " patches");
        return data;
    }

    private static PatchSite createPatchSite(List<InstallData> dataList) {
        Comparator<InstallData> comparator = Comparator.comparingInt((InstallData data) -> data.spec().priority()) //Sort by priority first
                .thenComparing(data -> data.spec().sourceMod().getName()); //Then alphabetically by mod name

        Patch[] before = dataList.stream()
                .filter(data -> data.spec().patchType() == PatchType.BEFORE)
                .sorted(comparator)
                .map(data -> new Patch(data.spec, data.handlerMethod))
                .toArray(Patch[]::new);

        Patch[] after = dataList.stream()
                .filter(data -> data.spec().patchType() == PatchType.AFTER)
                .sorted(comparator)
                .map(data -> new Patch(data.spec, data.handlerMethod))
                .toArray(Patch[]::new);

        return new PatchSite(before, after);
    }

    private static MethodHandle createMethodHandle(PatchSpec spec, ClassLoader loader) {
        try {
            Class<?> handlerClass = Class.forName(spec.handlerClass(), false, loader);
            Method handlerMethod = handlerClass.getDeclaredMethod(spec.handlerMethod(), PatchContext.class);
            handlerMethod.setAccessible(true);
            return MethodHandles.lookup().unreflect(handlerMethod);

        } catch (ClassNotFoundException e) {
            PatchLibLogger.error("Could not resolve class for " + spec.handlerClass() + ", skipping patch");
            return null;
        } catch (NoSuchMethodException e) {
            PatchLibLogger.error("Could not resolve method handle for " + spec.handlerClass() + " (" + spec.handlerMethod() + ")" + ", skipping patch");
            return null;
        } catch (IllegalAccessException e) {
            PatchLibLogger.error("Could not make method accessible for class " + spec.handlerClass() + " (" + spec.handlerMethod() + ")" + ", skipping patch");
            return null;
        }
    }

    private static Class<?> pickTemplate(MethodDescription method) {
        if (method.isConstructor()) return ConstructorTemplate.class;
        if (method.getReturnType().represents(void.class)) return VoidTemplate.class;
        return ReturnTemplate.class;
    }

}
