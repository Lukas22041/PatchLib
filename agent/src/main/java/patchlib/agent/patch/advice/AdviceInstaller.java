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
import patchlib.agent.patch.advice.templates.ConstructorTemplate;
import patchlib.agent.patch.advice.templates.ReturnTemplate;
import patchlib.agent.patch.advice.templates.VoidTemplate;
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

        builder = builder.visit(mapping.to(pickTemplate(methodDescription)).on(ElementMatchers.is(methodDescription)));

        PatchLibLogger.info("Installed a hook patch site at " + typeDescription.getActualName() + " on method " + methodDescription.getActualName() + " (" + methodDescription.getDescriptor() + ")");

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
                typeDescription.getName(), methodDescription.getName(), methodDescription.getDescriptor());
    }


    private static Class<?> pickTemplate(MethodDescription methodDescription) {
        if (methodDescription.isConstructor()) return ConstructorTemplate.class;
        else if (methodDescription.getReturnType().represents(void.class)) return VoidTemplate.class;
        return ReturnTemplate.class;
    }

    private static AdviceSpec.AdviceType getAdviceType(InstallationData data) {
        return ((AdviceSpec) data.spec().patchSpec()).adviceType();
    }

}
