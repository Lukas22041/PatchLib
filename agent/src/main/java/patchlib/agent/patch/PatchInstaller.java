package patchlib.agent.patch;

import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.description.ByteCodeElement;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.matcher.ElementMatcher;
import net.bytebuddy.matcher.ElementMatchers;
import patchlib.agent.log.PatchLibLogger;
import patchlib.agent.matchers.ClassMatcher;
import patchlib.agent.matchers.IgnoreMatcher;
import patchlib.agent.matchers.MethodMatcher;
import patchlib.agent.patch.advice.AdviceInstaller;
import patchlib.agent.patch.redirect.RedirectInstaller;
import patchlib.agent.spec.PatchHandlerSpec;
import patchlib.api.store.PatchData;

import java.lang.instrument.Instrumentation;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import static net.bytebuddy.matcher.ElementMatchers.*;

public class PatchInstaller {

    public void install(Instrumentation instrumentation, ClassLoader modClassLoader, List<PatchHandlerSpec> specs) {

        PatchLibLogger.info("Starting patch installation");
        long start = System.currentTimeMillis();

        List<InstallationData> installationData = createInstallationData(specs, modClassLoader);
        ElementMatcher.Junction<TypeDescription> allMatcher = createTypeMatcher(installationData);

        AgentBuilder agentBuilder = new AgentBuilder.Default()
                //Disable class shape changes entirely
                .disableClassFormatChanges()
                //Causes already loaded classes to be re-transformed with the patches applied.
                .with(AgentBuilder.RedefinitionStrategy.RETRANSFORMATION)
                //POOL_FIRST can cause classes to be initialised, those classes might be patch targets, but the default byte buddy discovery strategy would skip them.
                .with(AgentBuilder.RedefinitionStrategy.DiscoveryStrategy.Reiterating.INSTANCE)
                //Read from the read bytes first, use reflection as a fallback for classes that can not be read that way, which is mostly Janino classes.
                //The reflection approach causes class loads and can run in to issues in annotated classes, so it's just a fallback, not the main approach.
                .with(AgentBuilder.DescriptionStrategy.Default.POOL_FIRST)
                //Dont perform class shape changes
                .with(AgentBuilder.TypeStrategy.Default.DECORATE)
                .with(AgentBuilder.InitializationStrategy.NoOp.INSTANCE)
                //Caches discovered types, makes installation much faster but adds a permanent increase in memory used.
                .with(new AgentBuilder.PoolStrategy.WithTypePoolCache.Simple(new ConcurrentHashMap<>()))
                .with(new InstallListener())
                //Ignore specific classes for performance & stability reasons.
                .ignore(IgnoreMatcher.create())
                .type(allMatcher)
                .transform((builder, type, loader, module, protectionDomain) ->
                        transform(builder, type, installationData));


        agentBuilder.installOn(instrumentation);

        long diff = System.currentTimeMillis() - start;
        PatchLibLogger.info("Finished patch installation in " + diff + "ms. Note that this only includes installation on already loaded classes, not those that haven't loaded yet");
        PatchLibLogger.blank();

    }

    private DynamicType.Builder<?> transform(DynamicType.Builder<?> builder, TypeDescription type, List<InstallationData> installationDataList) {

        List<InstallationData> classmatchedData = new ArrayList<>();
        for (InstallationData installationData : installationDataList) {
            if (installationData.classMatcher().matches(type)) {
                classmatchedData.add(installationData);
            }
        }

        if (classmatchedData.isEmpty()) return builder;

        for (MethodDescription.InDefinedShape methodDescription : type.getDeclaredMethods()) {

            //Skip invalid patch targets
            if (methodDescription.isAbstract() || methodDescription.isNative()) continue;

            List<InstallationData> methodMatchedData = classmatchedData.stream().filter(data -> data.methodMatcher().matches(methodDescription)).toList();

            List<InstallationData> advicePatches = methodMatchedData.stream().filter(data -> data.spec().isAdvice()).toList();
            List<InstallationData> redirectPatches = methodMatchedData.stream().filter(data -> !data.spec().isAdvice()).toList();

            if (!advicePatches.isEmpty()) {
                builder = AdviceInstaller.transform(builder, type, methodDescription, advicePatches);
            }

            if (!redirectPatches.isEmpty()) {
                builder = RedirectInstaller.transform(builder, type, methodDescription, redirectPatches);
            }
        }

        return builder;
    }



    private ElementMatcher.Junction<TypeDescription> createTypeMatcher(List<InstallationData> installationDataList) {
        ElementMatcher.Junction<TypeDescription> allMatcher = none();
        for (InstallationData data : installationDataList) {
            allMatcher = allMatcher.or(data.classMatcher());
        }
        return allMatcher;
    }

    private List<InstallationData> createInstallationData(List<PatchHandlerSpec> specs, ClassLoader modClassLoader) {
        List<InstallationData> dataList = new ArrayList<>();
        for (PatchHandlerSpec spec : specs) {
            try {
                MethodHandle handle = createMethodHandle(spec, modClassLoader);
                if (handle == null) continue;

                String errorMessage = "Ran in to some issue while executing " + spec.handlerMethodName() + " in " + spec.handlerClassName() + " from "
                        + spec.sourceMod().getName();

                InstallationData data = new InstallationData(spec, handle, ClassMatcher.fromQuery(spec.targetClass()), MethodMatcher.fromQuery(spec.targetMethod()), errorMessage);
                dataList.add(data);
            } catch (Throwable ex) {
                PatchLibLogger.error("Failed creating installation data for handler method" + spec.handlerMethodName() + " in " + spec.handlerClassName() + " from mod " + spec.sourceMod().getName(), ex);
            }
        }
        dataList = sortInstallationData(dataList);
        PatchLibLogger.info("Collected " + dataList.size() + " patches");
        return dataList;
    }

    /** Sort based on priority, then by mod name. Execution order for patches is based on this. */
    private List<InstallationData> sortInstallationData(List<InstallationData> installationData) {
        return installationData.stream()
                .sorted(Comparator.comparingInt((InstallationData data) -> data.spec().priority())
                        .thenComparing((InstallationData data) -> data.spec().sourceMod().getName())
                )
                .toList();
    }

    private MethodHandle createMethodHandle(PatchHandlerSpec spec, ClassLoader modClassLoader) {

        try {
            Class<?> contextClass = spec.getContextClass();
            Class<?> handlerClass = Class.forName(spec.handlerClassName(), false, modClassLoader);

            Method method = handlerClass.getDeclaredMethod(spec.handlerMethodName(), contextClass);
            if (!Modifier.isStatic(method.getModifiers())) {
                PatchLibLogger.error("Handler method " + spec.handlerMethodName() + " in " + spec.handlerClassName() + " from mod " + spec.sourceMod().getName()
                        + " is not static, skipped");
                return null;
            }
            method.setAccessible(true);
            MethodHandle methodHandle = MethodHandles.lookup().unreflect(method);
            methodHandle = methodHandle.asType(MethodType.methodType(void.class, spec.getContextImplClass()));
            return methodHandle;

        } catch (ClassNotFoundException ex) {
            PatchLibLogger.error("Could not resolve handler class " + spec.handlerClassName() + " from mod " + spec.sourceMod().getName(), ex);
            return null;
        } catch (NoSuchMethodException ex) {
            PatchLibLogger.error("Could not resolve handler method" + spec.handlerMethodName() + " in " + spec.handlerClassName() + " from mod " + spec.sourceMod().getName(), ex);
            return null;
        } catch (IllegalAccessException ex) {
            PatchLibLogger.error("Failed accessing handler method" + spec.handlerMethodName() + " in " + spec.handlerClassName() + " from mod " + spec.sourceMod().getName(), ex);
            return null;
        }
    }


}
