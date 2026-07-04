package patchlib.agent.patch;

import net.bytebuddy.asm.Advice;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.matcher.ElementMatchers;
import net.bytebuddy.utility.JavaConstant;
import patchlib.agent.PatchHandler;
import patchlib.agent.PatchLibLogger;
import patchlib.agent.PatchRegistry;
import patchlib.agent.PatchSite;
import patchlib.agent.dispatch.AfterChainMarker;
import patchlib.agent.dispatch.BeforeChainMarker;
import patchlib.agent.dispatch.ChainBootstrap;
import patchlib.agent.dispatch.DispatchIdMarker;
import patchlib.agent.patch.template.ConstantEnterTemplates;
import patchlib.agent.patch.template.ConstantExitTemplates;
import patchlib.agent.patch.template.EnterTemplates;
import patchlib.agent.patch.template.ExitTemplates;
import patchlib.agent.spec.PatchType;

import java.lang.invoke.MethodHandles;
import java.lang.reflect.Method;
import java.util.Comparator;
import java.util.List;

/** Installs the before/after/except patches of one host method. Registers the site with its priority-ordered
 * handlers, then wraps the method body with the leanest advice template pair the site needs.
 * Hosts with class file version 55 or newer get the constant dispatch templates, where the handler
 * chains are dynamic constants the JIT can inline. Older hosts (mostly janino compiled scripts)
 * keep the id-based legacy dispatch. */
final class AdviceInstaller {

    private AdviceInstaller() {}

    /** The constant dynamic bootstrap behind every chain constant, see ChainBootstrap. */
    private static final Method BOOTSTRAP = resolveBootstrap();

    static DynamicType.Builder<?> install(DynamicType.Builder<?> builder, TypeDescription type,
                                          MethodDescription.InDefinedShape method, List<InstallData> advice) {
        int id = PatchRegistry.register(PatchInstaller.memberKey(method), createPatchSite(advice));
        boolean hasBefore = advice.stream().anyMatch(data -> data.spec().patchType() == PatchType.BEFORE);
        boolean hasExcept = advice.stream().anyMatch(data -> data.spec().patchType() == PatchType.EXCEPT);
        boolean constants = PatchInstaller.supportsConstants(type);

        if (constants) {
            //Binding both chains and the id is fine, only what a template declares lands in the class.
            //The id is used by the Except templates, which dispatch the throw path like legacy dispatch.
            builder = builder.visit(
                    Advice.withCustomMapping()
                            .bind(BeforeChainMarker.class, JavaConstant.Dynamic.bootstrap("before", BOOTSTRAP, id))
                            .bind(AfterChainMarker.class, JavaConstant.Dynamic.bootstrap("after", BOOTSTRAP, id))
                            .bind(DispatchIdMarker.class, id)
                            .to(pickConstantEnter(method, hasBefore), pickConstantExit(method, hasExcept))
                            .on(ElementMatchers.is(method))
            );
        } else {
            builder = builder.visit(
                    Advice.withCustomMapping()
                            .bind(DispatchIdMarker.class, id) //Attach the Dispatch ID
                            .to(pickEnter(method, hasBefore), pickExit(method, hasExcept)) //Pick the leanest template pair for the site
                            .on(ElementMatchers.is(method))
            );
        }

        PatchLibLogger.info("Installed a patch site at " + type.getActualName() + " for method " + method.getActualName()
                + (constants ? " (constant dispatch)" : " (legacy dispatch)"));
        return builder;
    }

    private static PatchSite createPatchSite(List<InstallData> dataList) {
        return new PatchSite(
                patchesOf(dataList, PatchType.BEFORE),
                patchesOf(dataList, PatchType.AFTER),
                patchesOf(dataList, PatchType.EXCEPT));
    }

    /** All patches of one type, ordered by priority, ties broken alphabetically by mod name. */
    private static PatchHandler[] patchesOf(List<InstallData> dataList, PatchType type) {
        return dataList.stream()
                .filter(data -> data.spec().patchType() == type)
                .sorted(Comparator.comparingInt((InstallData data) -> data.spec().priority())
                        .thenComparing(data -> data.spec().sourceMod().getName()))
                .map(data -> new PatchHandler(data.spec(), data.handlerMethod(), data.blame()))
                .toArray(PatchHandler[]::new);
    }

    private static Class<?> pickEnter(MethodDescription method, boolean hasBefore) {
        if (method.isConstructor())
            return hasBefore ? EnterTemplates.ConstructorWithBefore.class : EnterTemplates.ConstructorPlain.class;
        return hasBefore ? EnterTemplates.WithBefore.class : EnterTemplates.Plain.class;
    }

    private static Class<?> pickExit(MethodDescription method, boolean hasExcept) {
        if (method.isConstructor()) return ExitTemplates.Constructor.class;
        if (method.getReturnType().represents(void.class))
            return hasExcept ? ExitTemplates.VoidExcept.class : ExitTemplates.NoValue.class;
        return hasExcept ? ExitTemplates.ValueExcept.class : ExitTemplates.Value.class;
    }

    private static Class<?> pickConstantEnter(MethodDescription method, boolean hasBefore) {
        if (method.isConstructor())
            return hasBefore ? ConstantEnterTemplates.ConstructorWithBefore.class : ConstantEnterTemplates.ConstructorPlain.class;
        return hasBefore ? ConstantEnterTemplates.WithBefore.class : ConstantEnterTemplates.Plain.class;
    }

    private static Class<?> pickConstantExit(MethodDescription method, boolean hasExcept) {
        if (method.isConstructor()) return ConstantExitTemplates.Constructor.class;
        if (method.getReturnType().represents(void.class))
            return hasExcept ? ConstantExitTemplates.VoidExcept.class : ConstantExitTemplates.NoValue.class;
        return hasExcept ? ConstantExitTemplates.ValueExcept.class : ConstantExitTemplates.Value.class;
    }

    private static Method resolveBootstrap() {
        try {
            return ChainBootstrap.class.getMethod("bootstrap",
                    MethodHandles.Lookup.class, String.class, Class.class, int.class);
        } catch (NoSuchMethodException e) {
            throw new RuntimeException("Could not resolve the chain bootstrap", e);
        }
    }
}
