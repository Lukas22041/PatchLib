package patchlib.agent.patch;

import net.bytebuddy.asm.Advice;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.matcher.ElementMatchers;
import patchlib.agent.PatchHandler;
import patchlib.agent.PatchLibLogger;
import patchlib.agent.PatchRegistry;
import patchlib.agent.PatchSite;
import patchlib.agent.dispatch.DispatchIdMarker;
import patchlib.agent.patch.template.EnterTemplates;
import patchlib.agent.patch.template.ExitTemplates;
import patchlib.agent.spec.PatchType;

import java.util.Comparator;
import java.util.List;

/** Installs the before/after/except patches of one host method. Registers the site with its priority-ordered
 * handlers, then wraps the method body with the leanest advice template pair the site needs. */
final class AdviceInstaller {

    private AdviceInstaller() {}

    static DynamicType.Builder<?> install(DynamicType.Builder<?> builder, TypeDescription type,
                                          MethodDescription.InDefinedShape method, List<InstallData> advice) {
        int id = PatchRegistry.register(PatchInstaller.memberKey(method), createPatchSite(advice));
        boolean hasBefore = advice.stream().anyMatch(data -> data.spec().patchType() == PatchType.BEFORE);
        boolean hasExcept = advice.stream().anyMatch(data -> data.spec().patchType() == PatchType.EXCEPT);

        builder = builder.visit(
                Advice.withCustomMapping()
                        .bind(DispatchIdMarker.class, id) //Attach the Dispatch ID
                        .to(pickEnter(method, hasBefore), pickExit(method, hasExcept)) //Pick the leanest template pair for the site
                        .on(ElementMatchers.is(method))
        );

        PatchLibLogger.info("Installed a patch site at " + type.getActualName() + " for method " + method.getActualName());
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
                .map(data -> new PatchHandler(data.spec(), data.handlerMethod()))
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
}
