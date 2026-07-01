package patchlib.agent.patch;

import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.asm.AsmVisitorWrapper;
import net.bytebuddy.asm.MemberSubstitution;
import net.bytebuddy.description.field.FieldDescription;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.matcher.ElementMatcher;
import net.bytebuddy.matcher.ElementMatchers;
import net.bytebuddy.utility.JavaModule;
import patchlib.agent.Patch;
import patchlib.agent.PatchLibLogger;
import patchlib.agent.PatchRegistry;
import patchlib.agent.PatchSite;
import patchlib.agent.context.RedirectContextImpl;
import patchlib.agent.dispatch.DispatchIdMarker;
import patchlib.agent.matchers.ClassTargetMatcher;
import patchlib.agent.matchers.FieldTargetMatcher;
import patchlib.agent.matchers.GateMatcher;
import patchlib.agent.matchers.IgnoreMatcher;
import patchlib.agent.matchers.MethodTargetMatcher;
import patchlib.agent.matchers.SubtypeIndex;
import patchlib.agent.patch.template.ConstructorTemplate;
import patchlib.agent.patch.template.ReturnTemplate;
import patchlib.agent.patch.template.VoidTemplate;
import patchlib.agent.spec.PatchSpec;
import patchlib.agent.spec.PatchType;
import patchlib.agent.spec.RedirectKind;
import patchlib.api.context.AfterContext;
import patchlib.api.context.BeforeContext;
import patchlib.api.context.ExceptContext;
import patchlib.api.context.FieldReadContext;
import patchlib.api.context.FieldWriteContext;
import patchlib.api.context.MethodCallContext;
import patchlib.agent.context.PatchContext;

import java.lang.instrument.Instrumentation;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class PatchInstaller {

    /** Bundled data for the install process */
    private record InstallData(PatchSpec spec,
                               ElementMatcher<TypeDescription> classMatcher,
                               ElementMatcher<MethodDescription> methodMatcher,
                               MethodHandle handlerMethod) {}

    public static void install(Instrumentation inst, List<PatchSpec> specs, ClassLoader handlerLoader) {
        //Create all the data that will be used for the install process once
        List<InstallData> data = setupData(specs, handlerLoader);

        SubtypeIndex subtypeIndex = SubtypeIndex.build(specs);

        new AgentBuilder.Default()
                .disableClassFormatChanges()
                .with(AgentBuilder.RedefinitionStrategy.RETRANSFORMATION) //Enables transforming existing classes & registers itself for future re-transformations

                //The POOL_FIRST description strategy can rarely cause classes to be class-loaded through its own checks, bytebuddys circular dependency
                //check prevents those from being modified during retransform. This toggle ensures those cases are ran through install afterward.
                .with(AgentBuilder.RedefinitionStrategy.DiscoveryStrategy.Reiterating.INSTANCE)

                //POOL_FIRST gathers data from target classed through reading its bytes by default. For classes where it can not do that (usually just Janino loaded classes),
                //it falls back to using Reflection instead. Bytebuddy uses HYBRID by default, which would use Reflection for everything. That is not possible in this case, as that
                //causes starsectors reflection block to occur on annotated classes.
                .with(AgentBuilder.DescriptionStrategy.Default.POOL_FIRST)
                .with(AgentBuilder.TypeStrategy.Default.DECORATE) //Prevents bytebuddy from making changes to the classes shape, which would be incompatible with retransformation
                .with(AgentBuilder.InitializationStrategy.NoOp.INSTANCE)
                .with(new AgentBuilder.PoolStrategy.WithTypePoolCache.Simple(new ConcurrentHashMap<>())) //Caches discovered types, prevents recursive subtype lookup from being very slow
                .with(new AgentBuilder.Listener.Adapter() {
                    @Override
                    public void onError(String typeName, ClassLoader classLoader, JavaModule module, boolean loaded, Throwable throwable) {
                        PatchLibLogger.error("Skipping patching " + typeName + " due to the thrown exception: " + throwable);
                    }
                })
                .ignore(IgnoreMatcher.create()) //Ignore attempts to patch core JVM, bytebuddy and PatchLib methods.
                .type(GateMatcher.create(specs, subtypeIndex)) //Filter out classes that aren't relevant to any patch

                //Transform is called in three scenarios for classes that get through the two filters above
                // - Once for every already loaded class on install
                // - Once every time a new class is loaded after install
                // - Every time a class is re-transformed by another source.
                .transform((builder, type, loader, module, pd) -> transform(builder, type, data))
                .installOn(inst);
    }

    /** Applies the bytecode change per method once.
     * Before/after/except patches insert calls to PatchDispatchers enter and exit via Advice. Redirect patches insert
     * a MemberSubstitution around the targeted call inside the method body. Both install on the same builder. */
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

            List<InstallData> advice = new ArrayList<>();
            List<InstallData> redirects = new ArrayList<>();
            for (InstallData data : forType) {
                if (!data.methodMatcher.matches(method)) continue;
                if (data.spec().patchType() == PatchType.REDIRECT) redirects.add(data);
                else advice.add(data);
            }

            if (!advice.isEmpty()) {
                String key = type.getName() + "#" + method.getInternalName() + method.getDescriptor();
                int id = PatchRegistry.register(key, createPatchSite(advice));

                builder = builder.visit(
                        Advice.withCustomMapping()
                                .bind(DispatchIdMarker.class, id) //Attach the Dispatch ID
                                .to(pickTemplate(method)) //Pick the right template per method type
                                .on(ElementMatchers.is(method))
                );

                PatchLibLogger.info("Installed a patch site at " + type.getActualName() + " for method " + method.getActualName());
            }

            if (!redirects.isEmpty()) {
                builder = installRedirects(builder, type, method, redirects);
            }
        }
        return builder;
    }

    /** Installs the redirects for a host method. All redirects of one kind share a single MemberSubstitution whose
     * matcher is the union of their call matchers. Grouping into layered sites happens during instrumentation, where
     * the resolved call is known, so divergent queries on the same call collapse together (see RedirectSubstitutionFactory). */
    private static DynamicType.Builder<?> installRedirects(DynamicType.Builder<?> builder, TypeDescription type,
                                                           MethodDescription.InDefinedShape method, List<InstallData> redirects) {
        String hostKey = type.getName() + "#" + method.getInternalName() + method.getDescriptor();

        Map<RedirectKind, List<InstallData>> byKind = new EnumMap<>(RedirectKind.class);
        for (InstallData data : redirects) {
            byKind.computeIfAbsent(data.spec().redirectSite().kind(), k -> new ArrayList<>()).add(data);
        }

        for (Map.Entry<RedirectKind, List<InstallData>> entry : byKind.entrySet()) {
            builder = builder.visit(redirectVisitor(entry.getKey(), entry.getValue(), hostKey, type, method));
            PatchLibLogger.info("Installed redirects (" + entry.getKey() + ") at " + type.getActualName() + " in method " + method.getActualName());
        }
        return builder;
    }

    /** Builds one MemberSubstitution for a kind of redirect in a host method. The selector matches any call any of the
     * redirects wants; the factory then groups them per resolved call and delegates to the matching bridge. */
    private static AsmVisitorWrapper redirectVisitor(RedirectKind kind, List<InstallData> kindData, String hostKey,
                                                     TypeDescription hostType, MethodDescription method) {
        List<RedirectSubstitutionFactory.Layer> layers = new ArrayList<>();
        MemberSubstitution.WithoutSpecification<MemberSubstitution.Target.ForMember> target;

        if (kind == RedirectKind.METHOD_CALL) {
            ElementMatcher.Junction<MethodDescription> selector = ElementMatchers.none();
            for (InstallData data : kindData) {
                ElementMatcher.Junction<MethodDescription> matcher = MethodTargetMatcher.create(data.spec().redirectSite());
                selector = selector.or(matcher);
                layers.add(new RedirectSubstitutionFactory.Layer(
                        member -> member instanceof MethodDescription md && matcher.matches(md),
                        new Patch(data.spec(), data.handlerMethod())));
            }
            target = MemberSubstitution.relaxed().method(selector);
        } else {
            ElementMatcher.Junction<FieldDescription> selector = ElementMatchers.none();
            for (InstallData data : kindData) {
                ElementMatcher.Junction<FieldDescription> matcher = FieldTargetMatcher.create(data.spec().redirectSite());
                selector = selector.or(matcher);
                layers.add(new RedirectSubstitutionFactory.Layer(
                        member -> member instanceof FieldDescription fd && matcher.matches(fd),
                        new Patch(data.spec(), data.handlerMethod())));
            }
            target = kind == RedirectKind.FIELD_READ
                    ? MemberSubstitution.relaxed().field(selector).onRead()
                    : MemberSubstitution.relaxed().field(selector).onWrite();
        }

        RedirectSubstitutionFactory factory = new RedirectSubstitutionFactory(kind, hostKey, hostType, bridgeFor(kind), layers);
        return target.replaceWith(factory).on(ElementMatchers.is(method));
    }

    private static Method bridgeFor(RedirectKind kind) {
        try {
            return switch (kind) {
                case METHOD_CALL -> RedirectBridges.class.getMethod("methodCall",
                        int.class, Class.class, MethodHandle.class, Object.class, Object[].class, Object.class, Object[].class);
                case FIELD_READ -> RedirectBridges.class.getMethod("fieldRead",
                        int.class, Class.class, MethodHandle.class, Object.class, Object.class, Object[].class);
                case FIELD_WRITE -> RedirectBridges.class.getMethod("fieldWrite",
                        int.class, Class.class, MethodHandle.class, Object.class, Object[].class, Object.class, Object[].class);
            };
        } catch (NoSuchMethodException e) {
            throw new RuntimeException("Could not resolve redirect bridge for " + kind, e);
        }
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
        Comparator<InstallData> comparator = priorityOrder();

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

        Patch[] except = dataList.stream()
                .filter(data -> data.spec().patchType() == PatchType.EXCEPT)
                .sorted(comparator)
                .map(data -> new Patch(data.spec, data.handlerMethod))
                .toArray(Patch[]::new);

        return new PatchSite(before, after, except);
    }

    private static Comparator<InstallData> priorityOrder() {
        return Comparator.comparingInt((InstallData data) -> data.spec().priority()) //Sort by priority first
                .thenComparing(data -> data.spec().sourceMod().getName()); //Then alphabetically by mod name
    }

    private static MethodHandle createMethodHandle(PatchSpec spec, ClassLoader loader) {
        Class<?> expectedContext = expectedContext(spec);
        try {
            Class<?> handlerClass = Class.forName(spec.handlerClass(), false, loader);
            Method handlerMethod = handlerClass.getDeclaredMethod(spec.handlerMethod(), expectedContext);
            handlerMethod.setAccessible(true);
            MethodHandle handle =  MethodHandles.lookup().unreflect(handlerMethod);
            return handle.asType(MethodType.methodType(void.class, contextImpl(spec)));

        } catch (ClassNotFoundException e) {
            PatchLibLogger.error("Could not resolve class for " + spec.handlerClass() + ", skipping patch");
            return null;
        } catch (NoSuchMethodException e) {
            PatchLibLogger.error("A " + spec.patchType() + " patch must take a single " + expectedContext.getSimpleName()
                    + " parameter, but " + spec.handlerClass() + " (" + spec.handlerMethod() + ") does not, skipping patch");
            return null;
        } catch (IllegalAccessException e) {
            PatchLibLogger.error("Could not make method accessible for class " + spec.handlerClass() + " (" + spec.handlerMethod() + ")" + ", skipping patch");
            return null;
        }
    }

    /** The context interface the handler method declares as its parameter. */
    private static Class<?> expectedContext(PatchSpec spec) {
        return switch (spec.patchType()) {
            case BEFORE -> BeforeContext.class;
            case AFTER -> AfterContext.class;
            case EXCEPT -> ExceptContext.class;
            case REDIRECT -> switch (spec.redirectSite().kind()) {
                case METHOD_CALL -> MethodCallContext.class;
                case FIELD_READ -> FieldReadContext.class;
                case FIELD_WRITE -> FieldWriteContext.class;
            };
        };
    }

    /** The concrete context the dispatcher passes, used as the normalized method handle type. */
    private static Class<?> contextImpl(PatchSpec spec) {
        return switch (spec.patchType()) {
            case BEFORE, AFTER, EXCEPT -> PatchContext.class;
            case REDIRECT -> RedirectContextImpl.class;
        };
    }

    private static Class<?> pickTemplate(MethodDescription method) {
        if (method.isConstructor()) return ConstructorTemplate.class;
        if (method.getReturnType().represents(void.class)) return VoidTemplate.class;
        return ReturnTemplate.class;
    }

}
