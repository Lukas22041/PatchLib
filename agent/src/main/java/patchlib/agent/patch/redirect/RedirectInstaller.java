package patchlib.agent.patch.redirect;

import net.bytebuddy.asm.AsmVisitorWrapper;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.DynamicType;
import patchlib.agent.log.PatchLibLogger;
import patchlib.agent.patch.InstallationData;
import patchlib.agent.patch.PatchInstaller;
import patchlib.agent.spec.PatchHandlerSpec;

import java.util.List;

public class RedirectInstaller {

    public static DynamicType.Builder<?> transform(DynamicType.Builder<?> builder, TypeDescription typeDescription,
                                                   MethodDescription.InDefinedShape methodDescription, List<InstallationData> installationDataList) {


        if (!PatchInstaller.supportsConstantDynamic(typeDescription)) {
            PatchLibLogger.error("One ore multiple redirect patches tried modifying the class " + typeDescription.getActualName() + " on method " + methodDescription.getActualName() +
                    "that is compiled with an unsupported classfile version (i.e janino compiled code). Those patches are being skipped for this target.");
            return builder;
        }

        return builder;
    }

    private static AsmVisitorWrapper createRedirectVisitor(PatchHandlerSpec.RedirectType redirectType, List<InstallationData> installationDataList) {

    }

    private static AsmVisitorWrapper createCallVisitor(List<InstallationData> callDataList, TypeDescription typeDescription) {

    }

    private static AsmVisitorWrapper createConstructorVisitor(List<InstallationData> callDataList, TypeDescription typeDescription) {

    }

    private static AsmVisitorWrapper createFieldReadVisitor(List<InstallationData> callDataList, TypeDescription typeDescription) {

    }

    private static AsmVisitorWrapper createFieldWriteVisitor(List<InstallationData> callDataList, TypeDescription typeDescription) {

    }

    private static RedirectSubstitutionFactory createSubstitutionFactory(PatchHandlerSpec.RedirectType redirectType, TypeDescription typeDescription,
                                                                         MethodDescription.InDefinedShape methodDescription, List<RedirectSubstitutionFactory.RedirectCandidate> candidates) {

    }

    private static List<InstallationData> filterDataByType(List<InstallationData> installationDataList) {

    }



}
