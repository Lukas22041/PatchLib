package patchlib.agent.patch.redirect;

import net.bytebuddy.asm.AsmVisitorWrapper;
import net.bytebuddy.asm.MemberSubstitution;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.matcher.ElementMatcher;
import net.bytebuddy.matcher.ElementMatchers;
import patchlib.agent.log.PatchLibLogger;
import patchlib.agent.matchers.MethodMatcher;
import patchlib.agent.patch.InstallationData;
import patchlib.agent.patch.PatchInstaller;
import patchlib.agent.spec.PatchHandlerSpec;
import patchlib.agent.spec.RedirectCallSpec;
import patchlib.api.spec.MethodQuerySpec;

import java.util.ArrayList;
import java.util.List;

public class RedirectInstaller {

    public static DynamicType.Builder<?> transform(DynamicType.Builder<?> builder, TypeDescription typeDescription,
                                                   MethodDescription.InDefinedShape methodDescription, List<InstallationData> installationDataList) {

        if (!PatchInstaller.supportsConstantDynamic(typeDescription)) {
            PatchLibLogger.error("One ore multiple redirect patches tried modifying the class " + typeDescription.getActualName() + " on method " + methodDescription.getActualName() +
                    "that is compiled with an unsupported classfile version (i.e janino compiled code). Those patches are being skipped for this target.");
            return builder;
        }

        AsmVisitorWrapper callVisitor = createRedirectVisitor(PatchHandlerSpec.RedirectType.METHOD_CALL, installationDataList, typeDescription, methodDescription);
        AsmVisitorWrapper constructorVisitor = createRedirectVisitor(PatchHandlerSpec.RedirectType.CONSTRUCTOR, installationDataList, typeDescription, methodDescription);
        AsmVisitorWrapper fieldReadVisitor = createRedirectVisitor(PatchHandlerSpec.RedirectType.FIELD_READ, installationDataList, typeDescription, methodDescription);
        AsmVisitorWrapper fieldWriteVisitor = createRedirectVisitor(PatchHandlerSpec.RedirectType.FIELD_WRITE, installationDataList, typeDescription, methodDescription);

        if (callVisitor != null) builder.visit(callVisitor);
        if (constructorVisitor != null) builder.visit(constructorVisitor);
        if (fieldReadVisitor != null) builder.visit(fieldReadVisitor);
        if (fieldWriteVisitor != null) builder.visit(fieldWriteVisitor);

        return builder;
    }

    private static AsmVisitorWrapper createRedirectVisitor(PatchHandlerSpec.RedirectType redirectType, List<InstallationData> installationDataList,
                                                           TypeDescription typeDescription, MethodDescription.InDefinedShape methodDescription) {

        List<InstallationData> typeData = filterDataByType(redirectType, installationDataList);
        if (typeData.isEmpty()) return null;

        return switch (redirectType) {
            case METHOD_CALL -> createCallVisitor(typeData, typeDescription, methodDescription);
            case CONSTRUCTOR -> createConstructorVisitor(typeData, typeDescription, methodDescription);
            case FIELD_READ -> createFieldReadVisitor(typeData, typeDescription, methodDescription);
            case FIELD_WRITE -> createFieldWriteVisitor(typeData, typeDescription, methodDescription);
        };
    }

    private static AsmVisitorWrapper createCallVisitor(List<InstallationData> callDataList, TypeDescription typeDescription, MethodDescription.InDefinedShape methodDescription) {
        List<RedirectSubstitutionFactory.RedirectCandidate> candidates = new ArrayList<>();
        //The matcher that decides which calls are processed by the factory
        ElementMatcher.Junction<MethodDescription> callMatcher = ElementMatchers.none();

        for (InstallationData data : callDataList) {
            RedirectCallSpec spec = (RedirectCallSpec) data.spec().patchSpec();
            ElementMatcher.Junction<MethodDescription> matcher = MethodMatcher.fromQuery(spec.call());
            callMatcher.or(matcher);
            RedirectSubstitutionFactory.RedirectCandidate candidate = new RedirectSubstitutionFactory.RedirectCandidate(member ->
                    member instanceof MethodDescription memberMethod && matcher.matches(memberMethod), data);
            candidates.add(candidate);
        }

        RedirectSubstitutionFactory factory = createSubstitutionFactory(PatchHandlerSpec.RedirectType.METHOD_CALL, typeDescription, methodDescription, candidates);

        return MemberSubstitution.relaxed() //Relaxed allows skipping safely over undecipherable elements
                .method(callMatcher) //Process any method call that matches any of the patches that affect this method
                .replaceWith(factory) //The factory that creates the replacement bytecode. This decides in detail which actual call gets replaced by which patch
                .on(ElementMatchers.is(methodDescription)); //Applied to the currently worked on method.

    }

    private static AsmVisitorWrapper createConstructorVisitor(List<InstallationData> constructorDataList, TypeDescription typeDescription, MethodDescription.InDefinedShape methodDescription) {

    }

    private static AsmVisitorWrapper createFieldReadVisitor(List<InstallationData> fieldReadDataList, TypeDescription typeDescription, MethodDescription.InDefinedShape methodDescription) {

    }

    private static AsmVisitorWrapper createFieldWriteVisitor(List<InstallationData> fieldWriteDataList, TypeDescription typeDescription, MethodDescription.InDefinedShape methodDescription) {

    }

    private static RedirectSubstitutionFactory createSubstitutionFactory(PatchHandlerSpec.RedirectType redirectType, TypeDescription typeDescription,
                                                                         MethodDescription.InDefinedShape methodDescription, List<RedirectSubstitutionFactory.RedirectCandidate> candidates) {

        String hostKey = RedirectPatchRegistry.getMemberKey(methodDescription);
        return new RedirectSubstitutionFactory(redirectType, hostKey, typeDescription, candidates);
    }

    private static List<InstallationData> filterDataByType(PatchHandlerSpec.RedirectType redirectType, List<InstallationData> installationDataList) {
        return installationDataList.stream().filter(data -> data.spec().isRedirect() && data.spec().getRedirectType().equals(redirectType)).toList();
    }



}
