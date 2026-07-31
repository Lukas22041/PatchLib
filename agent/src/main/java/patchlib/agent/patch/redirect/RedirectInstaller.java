package patchlib.agent.patch.redirect;

import net.bytebuddy.asm.AsmVisitorWrapper;
import net.bytebuddy.asm.MemberSubstitution;
import net.bytebuddy.description.field.FieldDescription;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.matcher.ElementMatcher;
import net.bytebuddy.matcher.ElementMatchers;
import patchlib.agent.log.PatchLibLogger;
import patchlib.agent.matchers.FieldMatcher;
import patchlib.agent.matchers.MethodMatcher;
import patchlib.agent.patch.InstallationData;
import patchlib.agent.patch.PatchInstaller;
import patchlib.agent.spec.*;

import java.util.ArrayList;
import java.util.List;

import static net.bytebuddy.matcher.ElementMatchers.*;

public class RedirectInstaller {

    public static DynamicType.Builder<?> transform(DynamicType.Builder<?> builder, TypeDescription typeDescription,
                                                   MethodDescription.InDefinedShape methodDescription, List<InstallationData> installationDataList) {

        if (!PatchInstaller.supportsConstantDynamic(typeDescription)) {
            PatchLibLogger.error("One or multiple redirect patches tried modifying the class " + typeDescription.getActualName() + " on method " + methodDescription.getActualName() +
                    "that is compiled with an unsupported classfile version (i.e janino compiled code). Those patches are being skipped for this target.");
            return builder;
        }

        AsmVisitorWrapper callVisitor = createRedirectVisitor(PatchHandlerSpec.RedirectType.METHOD_CALL, installationDataList, typeDescription, methodDescription);
        AsmVisitorWrapper constructorVisitor = createRedirectVisitor(PatchHandlerSpec.RedirectType.CONSTRUCTOR, installationDataList, typeDescription, methodDescription);
        AsmVisitorWrapper fieldReadVisitor = createRedirectVisitor(PatchHandlerSpec.RedirectType.FIELD_READ, installationDataList, typeDescription, methodDescription);
        AsmVisitorWrapper fieldWriteVisitor = createRedirectVisitor(PatchHandlerSpec.RedirectType.FIELD_WRITE, installationDataList, typeDescription, methodDescription);

        if (callVisitor != null) builder = builder.visit(callVisitor);
        if (constructorVisitor != null) builder = builder.visit(constructorVisitor);
        if (fieldReadVisitor != null) builder = builder.visit(fieldReadVisitor);
        if (fieldWriteVisitor != null) builder = builder.visit(fieldWriteVisitor);

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
        ElementMatcher.Junction<MethodDescription> anyMatcher = ElementMatchers.none();

        for (InstallationData data : callDataList) {
            RedirectMethodCallSpec spec = (RedirectMethodCallSpec) data.spec().patchSpec();
            ElementMatcher.Junction<MethodDescription> matcher = MethodMatcher.fromQuery(spec.call(), spec.owner());
            anyMatcher = anyMatcher.or(matcher);
            RedirectSubstitutionFactory.RedirectCandidate candidate = new RedirectSubstitutionFactory.RedirectCandidate(member ->
                    member instanceof MethodDescription memberMethod && matcher.matches(memberMethod), data);
            candidates.add(candidate);
        }

        RedirectSubstitutionFactory factory = createSubstitutionFactory(PatchHandlerSpec.RedirectType.METHOD_CALL, typeDescription, methodDescription, candidates);

        return MemberSubstitution.relaxed() //Relaxed allows skipping safely over undecipherable elements
                .method(anyMatcher) //Process any method call that matches any of the patches that affect this method
                .replaceWith(factory) //The factory that creates the replacement bytecode. This decides in detail which actual call gets replaced by which patch
                .on(ElementMatchers.is(methodDescription)); //Applied to the currently worked on method.

    }

    private static AsmVisitorWrapper createConstructorVisitor(List<InstallationData> constructorDataList, TypeDescription typeDescription, MethodDescription.InDefinedShape methodDescription) {
        List<RedirectSubstitutionFactory.RedirectCandidate> candidates = new ArrayList<>();
        //The matcher that decides which calls are processed by the factory
        ElementMatcher.Junction<MethodDescription> anyMatcher = ElementMatchers.none();

        for (InstallationData data : constructorDataList) {
            RedirectConstructorCallSpec spec = (RedirectConstructorCallSpec) data.spec().patchSpec();
            ElementMatcher.Junction<MethodDescription> matcher = MethodMatcher.fromQuery(spec.constructor(), spec.constructed());
            anyMatcher = anyMatcher.or(matcher);
            RedirectSubstitutionFactory.RedirectCandidate candidate = new RedirectSubstitutionFactory.RedirectCandidate(member ->
                    member instanceof MethodDescription memberMethod && matcher.matches(memberMethod), data);
            candidates.add(candidate);
        }

        //this() and super() calls have the same bytecode invokespecial shape as a "new" instruction
        //So this code prevents those from being potentially targeted by a new redirect, by checking that they arent
        //The targeted classes constructors.
        if (methodDescription.isConstructor()) {
            anyMatcher = anyMatcher.and(not(isDeclaredBy(typeDescription)));
            TypeDescription.Generic superClass = typeDescription.getSuperClass();
            if (superClass != null) anyMatcher = anyMatcher.and(not(isDeclaredBy(superClass.asErasure())));
        }

        RedirectSubstitutionFactory factory = createSubstitutionFactory(PatchHandlerSpec.RedirectType.CONSTRUCTOR, typeDescription, methodDescription, candidates);

        return MemberSubstitution.relaxed()
                .constructor(anyMatcher)
                .replaceWith(factory)
                .on(ElementMatchers.is(methodDescription));

    }

    private static AsmVisitorWrapper createFieldReadVisitor(List<InstallationData> fieldReadDataList, TypeDescription typeDescription, MethodDescription.InDefinedShape methodDescription) {
        List<RedirectSubstitutionFactory.RedirectCandidate> candidates = new ArrayList<>();
        ElementMatcher.Junction<FieldDescription> anyMatcher = ElementMatchers.none();

        for (InstallationData data : fieldReadDataList) {
            RedirectFieldReadSpec spec = (RedirectFieldReadSpec) data.spec().patchSpec();
            ElementMatcher.Junction<FieldDescription> matcher = FieldMatcher.fromQuery(spec.field(), spec.owner());
            anyMatcher = anyMatcher.or(matcher);
            RedirectSubstitutionFactory.RedirectCandidate candidate = new RedirectSubstitutionFactory.RedirectCandidate(member ->
                    member instanceof FieldDescription field && matcher.matches(field), data);
            candidates.add(candidate);
        }

        RedirectSubstitutionFactory factory = createSubstitutionFactory(PatchHandlerSpec.RedirectType.FIELD_READ, typeDescription, methodDescription, candidates);

        return MemberSubstitution.relaxed()
                .field(anyMatcher).onRead()
                .replaceWith(factory)
                .on(ElementMatchers.is(methodDescription));
    }

    private static AsmVisitorWrapper createFieldWriteVisitor(List<InstallationData> fieldWriteDataList, TypeDescription typeDescription, MethodDescription.InDefinedShape methodDescription) {
        List<RedirectSubstitutionFactory.RedirectCandidate> candidates = new ArrayList<>();
        ElementMatcher.Junction<FieldDescription> anyMatcher = ElementMatchers.none();

        for (InstallationData data : fieldWriteDataList) {
            RedirectFieldWriteSpec spec = (RedirectFieldWriteSpec) data.spec().patchSpec();
            ElementMatcher.Junction<FieldDescription> matcher = FieldMatcher.fromQuery(spec.field(), spec.owner());
            anyMatcher = anyMatcher.or(matcher);
            RedirectSubstitutionFactory.RedirectCandidate candidate = new RedirectSubstitutionFactory.RedirectCandidate(member ->
                    member instanceof FieldDescription field && matcher.matches(field), data);
            candidates.add(candidate);
        }

        RedirectSubstitutionFactory factory = createSubstitutionFactory(PatchHandlerSpec.RedirectType.FIELD_WRITE, typeDescription, methodDescription, candidates);

        return MemberSubstitution.relaxed()
                .field(anyMatcher).onWrite()
                .replaceWith(factory)
                .on(ElementMatchers.is(methodDescription));
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
