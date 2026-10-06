package patchlib.agent.patch.advice;

import net.bytebuddy.asm.Advice;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.implementation.bytecode.constant.NullConstant;
import net.bytebuddy.matcher.ElementMatcher;
import net.bytebuddy.matcher.ElementMatchers;
import net.bytebuddy.utility.JavaConstant;
import patchlib.agent.context.HookContextImpl;
import patchlib.agent.log.PatchLibLogger;
import patchlib.agent.patch.InstallationData;
import patchlib.agent.patch.PatchInstaller;
import patchlib.agent.patch.SiteIdMarker;
import patchlib.agent.patch.advice.templates.*;
import patchlib.agent.spec.AdviceSpec;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodType;
import java.util.List;

public class AdviceInstaller {


    public static DynamicType.Builder<?> transform(DynamicType.Builder<?> builder, TypeDescription typeDescription,
                                                   MethodDescription.InDefinedShape methodDescription, List<InstallationData> installationDataList) {

        String memberKey = AdvicePatchRegistry.getSiteKey(methodDescription);
        AdvicePatchSite site = createAdvicePatchSite(typeDescription, methodDescription, installationDataList);
        int patchId = AdvicePatchRegistry.register(memberKey, site);

        boolean supportsConstantDynamics = PatchInstaller.supportsConstantDynamic(typeDescription);

        Advice.WithCustomMapping mapping = Advice.withCustomMapping()
                .bind(SiteIdMarker.class, patchId);


        //By default, use constant dynamics, which involves having bytebuddy register them in the classes constant pool,
        //which then calls the refered to bootstrap method. This allows the Just-in-Time compiler to optimise the code.
        if (supportsConstantDynamics) {
            mapping = mapping.bind(BeforeHandleMarker.class, JavaConstant.Dynamic.bootstrap(AdviceBootstrap.BEFORE, AdviceBootstrap.BOOTSTRAP_METHOD, patchId))
                    .bind(AfterHandleMarker.class, JavaConstant.Dynamic.bootstrap(AdviceBootstrap.AFTER, AdviceBootstrap.BOOTSTRAP_METHOD, patchId));
        }
        //Older versions of classes do not yet support constant dynamics, and janino is marked as an older class file format, so
        //for the fallback just use the passed in SiteIdMarker to grab the handle chain at runtime.
        else {
            mapping = mapping.bind(BeforeHandleMarker.class, NullConstant.INSTANCE, MethodHandle.class)
                    .bind(AfterHandleMarker.class, NullConstant.INSTANCE, MethodHandle.class);
        }

        if (methodDescription.isConstructor()) {
            warnAboutConstructorExcepts(typeDescription, site);
        }

        Class<?> enterTemplate = pickEnterTemplate(methodDescription, site);
        Class<?> exitTemplate = pickExitTemplate(methodDescription, site);
        if (enterTemplate == null && exitTemplate == null) return builder;

        Advice advice;
        if (enterTemplate == null) advice = mapping.to(exitTemplate);
        else if (exitTemplate == null) advice = mapping.to(enterTemplate);
        else advice = mapping.to(enterTemplate, exitTemplate);

        builder = builder.visit(advice.on(ElementMatchers.is(methodDescription)));

        PatchLibLogger.info("Installed a hook patch site at " + typeDescription.getActualName() + " on method " + methodDescription.getActualName() + " " + methodDescription.getParameters() + "");

        return builder;
    }

    private static AdvicePatchSite createAdvicePatchSite(TypeDescription typeDescription, MethodDescription.InDefinedShape methodDescription,
                                                         List<InstallationData> installationDataList) {

        List<InstallationData> beforeData = installationDataList.stream().filter(data -> getAdviceType(data) == AdviceSpec.AdviceType.BEFORE).toList();
        List<InstallationData> afterData = installationDataList.stream().filter(data -> getAdviceType(data) == AdviceSpec.AdviceType.AFTER).toList();
        List<InstallationData> exceptData = installationDataList.stream().filter(data -> getAdviceType(data) == AdviceSpec.AdviceType.EXCEPT).toList();

        MethodHandle beforeChain = AdviceHandleChain.createHandleChain(beforeData);
        MethodHandle afterChain = AdviceHandleChain.createHandleChain(afterData);
        MethodHandle exceptChain = AdviceHandleChain.createHandleChain(exceptData);

        return new AdvicePatchSite(beforeChain, afterChain, exceptChain,beforeData, afterData, exceptData,
                typeDescription.getName(), methodDescription.getName(), methodDescription.getDescriptor(), methodDescription.isVirtual());
    }


    private static Class<?> pickEnterTemplate(MethodDescription methodDescription, AdvicePatchSite site) {
        if (!site.before.isEmpty()) return methodDescription.isConstructor() ? ConstructorBeforeTemplate.class : BeforeTemplate.class;
        if (!site.after.isEmpty()) return ContextTemplate.class;
        //Except only, ExceptOnlyTemplate creates its own context
        return null;
    }

    /** The exit templates are also used for void methods, Byte Buddy reads their return value as null and ignores writes to it. */
    private static Class<?> pickExitTemplate(MethodDescription methodDescription, AdvicePatchSite site) {
        if (methodDescription.isConstructor()) return site.after.isEmpty() ? null : ConstructorAfterTemplate.class;
        if (!site.except.isEmpty()) return site.before.isEmpty() && site.after.isEmpty() ? ExceptOnlyTemplate.class : ExceptTemplate.class;
        if (!site.after.isEmpty()) return AfterTemplate.class;
        return methodDescription.getReturnType().represents(void.class) ? null : SkipTemplate.class;
    }

    /** Byte Buddy can not catch exceptions thrown in constructors, so @Except patches on them are skipped. */
    private static void warnAboutConstructorExcepts(TypeDescription typeDescription, AdvicePatchSite site) {
        for (InstallationData data : site.except) {
            PatchLibLogger.warn("Skipped the @Except patch " + data.spec().handlerMethodName() + " in " + data.spec().handlerClassName() + " from "
                    + data.spec().sourceMod().getName() + ", as it targets a constructor of " + typeDescription.getActualName());
        }
    }

    private static AdviceSpec.AdviceType getAdviceType(InstallationData data) {
        return ((AdviceSpec) data.spec().patchSpec()).adviceType();
    }

}
