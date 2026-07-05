package patchlib.agent.patch;

import net.bytebuddy.ClassFileVersion;
import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.description.ByteCodeElement;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.utility.JavaModule;
import patchlib.agent.PatchLibLogger;
import patchlib.agent.PatchRegistry;
import patchlib.agent.context.AdviceContextImpl;
import patchlib.agent.context.RedirectContextImpl;
import patchlib.agent.matchers.ClassTargetMatcher;
import patchlib.agent.matchers.GateMatcher;
import patchlib.agent.matchers.IgnoreMatcher;
import patchlib.agent.matchers.MethodTargetMatcher;
import patchlib.agent.matchers.SubtypeIndex;
import patchlib.agent.spec.PatchSpec;
import patchlib.agent.spec.PatchType;
import patchlib.api.context.AfterContext;
import patchlib.api.context.BeforeContext;
import patchlib.api.context.ConstructorCallContext;
import patchlib.api.context.ExceptContext;
import patchlib.api.context.FieldReadContext;
import patchlib.api.context.FieldWriteContext;
import patchlib.api.context.MethodCallContext;

import java.lang.instrument.Instrumentation;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/** Sets up the ByteBuddy agent and routes every matched method to the two installers:
 * AdviceInstaller for before/after/except patches, RedirectInstaller for redirects. */
public class PatchInstaller {

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
            if (data.classMatcher().matches(type)) {
                forType.add(data);
            }
        }

        if (forType.isEmpty()) return builder; //Shouldn't happen due to the gate, but just in case.

        //Remember this class's supertypes so the debug menu can offer a group-by-supertype view.
        PatchRegistry.recordSupertypes(type.getName(), collectSupertypes(type));

        for (MethodDescription.InDefinedShape method : type.getDeclaredMethods()) {

            if (method.isAbstract() || method.isNative()) continue; //Methods with no body to install in to, skip.

            List<InstallData> advice = new ArrayList<>();
            List<InstallData> redirects = new ArrayList<>();
            for (InstallData data : forType) {
                if (!data.methodMatcher().matches(method)) continue;
                if (data.spec().patchType() == PatchType.REDIRECT) redirects.add(data);
                else advice.add(data);
            }

            if (!advice.isEmpty()) {
                builder = AdviceInstaller.install(builder, type, method, advice);
            }

            if (!redirects.isEmpty()) {
                builder = RedirectInstaller.install(builder, type, method, redirects);
            }
        }
        return builder;
    }

    /** The registry key of a member: declaring class, name and descriptor. Shared by advice sites,
     * redirect host methods and resolved redirect targets. */
    static String memberKey(ByteCodeElement.Member member) {
        return member.getDeclaringType().asErasure().getName() + "#" + member.getInternalName() + member.getDescriptor();
    }

    /** All supertypes of a class (transitive superclasses and interfaces), minus the class itself and Object.
     * Read from the type pool, so it describes types rather than loading them. Best effort on any failure. */
    private static Set<String> collectSupertypes(TypeDescription type) {
        Set<String> out = new LinkedHashSet<>();
        try {
            collectSupertypes(type, out);
        } catch (Throwable t) {
            PatchLibLogger.error("Could not read supertypes of " + type.getName() + ": " + t);
        }
        out.remove(type.getName());
        out.remove(Object.class.getName());
        return out;
    }

    private static void collectSupertypes(TypeDescription type, Set<String> out) {
        TypeDescription.Generic superClass = type.getSuperClass();
        if (superClass != null) {
            TypeDescription erasure = superClass.asErasure();
            if (out.add(erasure.getName())) collectSupertypes(erasure, out);
        }
        for (TypeDescription.Generic itf : type.getInterfaces()) {
            TypeDescription erasure = itf.asErasure();
            if (out.add(erasure.getName())) collectSupertypes(erasure, out);
        }
    }

    /** Dynamic constants need class file version 55 or newer in the host class. Janino compiled code is below this. */
    static boolean supportsConstants(TypeDescription type) {
        ClassFileVersion version = type.getClassFileVersion();
        return version != null && version.isAtLeast(ClassFileVersion.JAVA_V11);
    }

    private static List<InstallData> setupData(List<PatchSpec> specs, ClassLoader handlerLoader) {
        List<InstallData> data = new ArrayList<>();
        for (PatchSpec spec : specs) {
            //A broken patch is skipped and blamed; it must never take down the install of everyone elses patches.
            try {
                MethodHandle handle = createMethodHandle(spec, handlerLoader);
                if (handle == null) continue;

                //Built here, before the transformer installs, because reading the mod spec later
                //can call in to patched code and recurse back in to the dispatch that needed it.
                String blame = "Ran in to an error while dispatcher was executing "
                        + spec.handlerClass() + "#" + spec.handlerMethod() + " from mod " + spec.sourceMod().getId();

                data.add(new InstallData(
                        spec,
                        ClassTargetMatcher.create(spec.targetClass()),
                        MethodTargetMatcher.create(spec.targetMethod()),
                        handle,
                        blame));
            } catch (Throwable t) {
                PatchLibLogger.error("Could not set up " + spec.handlerClass() + " (" + spec.handlerMethod() + ") from mod "
                        + spec.sourceMod().getId() + ", skipping patch: " + t);
            }
        }
        PatchLibLogger.info("Assembled " + data.size() + " patches");
        return data;
    }

    private static MethodHandle createMethodHandle(PatchSpec spec, ClassLoader loader) {
        Class<?> expectedContext = expectedContext(spec);
        try {
            Class<?> handlerClass = Class.forName(spec.handlerClass(), false, loader);
            Method handlerMethod = handlerClass.getDeclaredMethod(spec.handlerMethod(), expectedContext);
            if (!Modifier.isStatic(handlerMethod.getModifiers())) {
                PatchLibLogger.error("Patch handlers must be static, but " + spec.handlerClass() + " (" + spec.handlerMethod() + ") is not, skipping patch");
                return null;
            }
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
                case CONSTRUCTOR -> ConstructorCallContext.class;
                case FIELD_READ -> FieldReadContext.class;
                case FIELD_WRITE -> FieldWriteContext.class;
            };
        };
    }

    /** The concrete context the dispatcher passes, used as the normalized method handle type. */
    private static Class<?> contextImpl(PatchSpec spec) {
        return switch (spec.patchType()) {
            case BEFORE, AFTER, EXCEPT -> AdviceContextImpl.class;
            case REDIRECT -> RedirectContextImpl.class;
        };
    }

}
