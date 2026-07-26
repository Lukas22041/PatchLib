package patchlib.agent.patch.advice;

import net.bytebuddy.asm.Advice;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.matcher.ElementMatcher;
import net.bytebuddy.matcher.ElementMatchers;
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

        builder = builder.visit(
                Advice.withCustomMapping()
                        .bind(SiteIdMarker.class, patchId)
                        .to(pickTemplate(methodDescription))
                        .on(ElementMatchers.is(methodDescription))
        );

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
