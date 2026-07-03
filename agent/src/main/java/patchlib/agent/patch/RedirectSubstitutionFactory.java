package patchlib.agent.patch;

import net.bytebuddy.asm.MemberSubstitution.Substitution;
import net.bytebuddy.asm.MemberSubstitution.Target;
import net.bytebuddy.description.ByteCodeElement;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.description.type.TypeList;
import net.bytebuddy.implementation.bytecode.StackManipulation;
import net.bytebuddy.implementation.bytecode.assign.Assigner;
import net.bytebuddy.pool.TypePool;
import net.bytebuddy.utility.JavaConstant;
import patchlib.agent.PatchHandler;
import patchlib.agent.PatchRegistry;
import patchlib.agent.RedirectSite;
import patchlib.agent.dispatch.DispatchIdMarker;
import patchlib.agent.dispatch.DispatchOwnerMarker;
import patchlib.agent.spec.RedirectKind;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Predicate;

/** Decides the redirect substitution per resolved call. MemberSubstitution calls resolve() once per matched call site
 * during instrumentation and hands us the actual resolved member, so the layers are grouped by the call they really
 * hit, not by how each query was phrased. Two patches that target the same call with different queries therefore
 * collapse to one site and nest, which is what the layered model promises.
 *
 * Once the matching layers are known, the real bytecode is left to ByteBuddys standard delegation chain. */
final class RedirectSubstitutionFactory implements Substitution.Factory<Target.ForMember> {

    /** One redirect: how to recognise its call site against a resolved member, and the handler to run. */
    record Layer(Predicate<ByteCodeElement.Member> matches, PatchHandler patch) {}

    private final RedirectKind kind;
    private final String hostKey;
    private final TypeDescription hostType;
    private final Method bridge;
    private final Method staticBridge; //replaces bridge when the resolved method is static, null for other kinds
    private final List<Layer> layers;

    RedirectSubstitutionFactory(RedirectKind kind, String hostKey, TypeDescription hostType, Method bridge, Method staticBridge, List<Layer> layers) {
        this.kind = kind;
        this.hostKey = hostKey;
        this.hostType = hostType;
        this.bridge = bridge;
        this.staticBridge = staticBridge;
        this.layers = layers;
    }

    @Override
    public Substitution<Target.ForMember> make(TypeDescription instrumentedType, MethodDescription instrumentedMethod, TypePool typePool) {
        return new SiteSubstitution(instrumentedType, instrumentedMethod, typePool);
    }

    /** The substitution applied at each matched call site. resolve() receives the resolved member behind the site
     * and returns the bytecode that replaces the original instruction. */
    private final class SiteSubstitution implements Substitution<Target.ForMember> {

        private final TypeDescription instrumentedType;
        private final MethodDescription instrumentedMethod;
        private final TypePool typePool;

        private SiteSubstitution(TypeDescription instrumentedType, MethodDescription instrumentedMethod, TypePool typePool) {
            this.instrumentedType = instrumentedType;
            this.instrumentedMethod = instrumentedMethod;
            this.typePool = typePool;
        }

        @Override
        public StackManipulation resolve(Target.ForMember target, TypeList.Generic parameters, TypeDescription.Generic result,
                                         JavaConstant.MethodHandle methodHandle, StackManipulation stackManipulation, int freeOffset) {
            ByteCodeElement.Member original = target.getMember();

            PatchHandler[] matched = matchedLayers(original);
            if (matched.length == 0) {
                //Not one of ours. Calls and field accesses take the raw instruction back unchanged, but a bound
                //constructor site can not (the visitor cleans the stack after it), so ByteBuddys own step rebuilds it.
                if (kind != RedirectKind.CONSTRUCTOR) return stackManipulation;
                return Substitution.Chain.<Target.ForMember>with(Assigner.DEFAULT, Assigner.Typing.DYNAMIC)
                        .executing(Substitution.Chain.Step.OfOriginalExpression.INSTANCE)
                        .make(instrumentedType, instrumentedMethod, typePool)
                        .resolve(target, parameters, result, methodHandle, stackManipulation, freeOffset);
            }

            //One site per resolved member per kind. Repeated calls and divergent queries share a key, which also keeps
            //registration idempotent if the class is retransformed. The kind is part of the key because a read and a
            //write of the same field resolve to the same member but need separate sites.
            int id = PatchRegistry.registerRedirect(hostKey + "->" + kind + ":" + PatchInstaller.memberKey(original),
                    new RedirectSite(matched, kind));

            //Static calls go through their own bridge variant, see RedirectBridges.methodCallStatic.
            Method chosenBridge = staticBridge != null && original instanceof MethodDescription method && method.isStatic()
                    ? staticBridge
                    : bridge;

            //Hand the actual bytecode back to ByteBuddys standard delegation, with this sites id baked in.
            return Substitution.Chain
                    .<Target.ForMember>with(Assigner.DEFAULT, Assigner.Typing.DYNAMIC)
                    .executing(Substitution.Chain.Step.ForDelegation
                            .withCustomMapping()
                            .bind(DispatchIdMarker.class, id)
                            .bind(DispatchOwnerMarker.class, hostType)
                            .to(chosenBridge))
                    .make(instrumentedType, instrumentedMethod, typePool)
                    .resolve(target, parameters, result, methodHandle, stackManipulation, freeOffset);
        }

        /** Reuses the matchers built at install to pick the layers that target this exact resolved call,
         * ordered by priority, ties broken alphabetically by mod name. */
        private PatchHandler[] matchedLayers(ByteCodeElement.Member original) {
            List<Layer> matched = new ArrayList<>();
            for (Layer layer : layers) {
                if (layer.matches().test(original)) matched.add(layer);
            }
            matched.sort(Comparator.comparingInt((Layer layer) -> layer.patch().spec().priority())
                    .thenComparing(layer -> layer.patch().spec().sourceMod().getName()));

            PatchHandler[] patches = new PatchHandler[matched.size()];
            for (int i = 0; i < patches.length; i++) patches[i] = matched.get(i).patch();
            return patches;
        }
    }
}
