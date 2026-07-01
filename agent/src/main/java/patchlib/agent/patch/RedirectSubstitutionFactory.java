package patchlib.agent.patch;

import net.bytebuddy.asm.MemberSubstitution.Substitution;
import net.bytebuddy.asm.MemberSubstitution.Target;
import net.bytebuddy.description.ByteCodeElement;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.implementation.bytecode.assign.Assigner;
import net.bytebuddy.pool.TypePool;
import patchlib.agent.Patch;
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
    record Layer(Predicate<ByteCodeElement.Member> matches, Patch patch) {}

    private final RedirectKind kind;
    private final String hostKey;
    private final TypeDescription hostType;
    private final Method bridge;
    private final List<Layer> layers;

    RedirectSubstitutionFactory(RedirectKind kind, String hostKey, TypeDescription hostType, Method bridge, List<Layer> layers) {
        this.kind = kind;
        this.hostKey = hostKey;
        this.hostType = hostType;
        this.bridge = bridge;
        this.layers = layers;
    }

    @Override
    public Substitution<Target.ForMember> make(TypeDescription instrumentedType, MethodDescription instrumentedMethod, TypePool typePool) {
        //Substitution has a single method, so this lambda is the substitution applied at each matched call site.
        return (target, parameters, result, methodHandle, stackManipulation, freeOffset) -> {
            ByteCodeElement.Member original = target.getMember();

            //Reuse the matchers built at install to pick the layers that target this exact resolved call.
            List<Layer> matched = new ArrayList<>();
            for (Layer layer : layers) {
                if (layer.matches().test(original)) matched.add(layer);
            }
            if (matched.isEmpty()) return stackManipulation; //Not one of ours, leave the call untouched.

            matched.sort(Comparator.comparingInt((Layer layer) -> layer.patch().spec().priority())
                    .thenComparing(layer -> layer.patch().spec().sourceMod().getName()));
            Patch[] patches = matched.stream().map(Layer::patch).toArray(Patch[]::new);

            //One site per resolved member per kind. Repeated calls and divergent queries share a key, which also keeps
            //registration idempotent if the class is retransformed. The kind is part of the key because a read and a
            //write of the same field resolve to the same member but need separate sites.
            int id = PatchRegistry.registerRedirect(hostKey + "->" + kind + ":" + memberKey(original), new RedirectSite(patches));

            //Hand the actual bytecode back to ByteBuddys standard delegation, with this sites id baked in.
            return Substitution.Chain
                    .<Target.ForMember>with(Assigner.DEFAULT, Assigner.Typing.DYNAMIC)
                    .executing(Substitution.Chain.Step.ForDelegation
                            .withCustomMapping()
                            .bind(DispatchIdMarker.class, id)
                            .bind(DispatchOwnerMarker.class, hostType)
                            .to(bridge))
                    .make(instrumentedType, instrumentedMethod, typePool)
                    .resolve(target, parameters, result, methodHandle, stackManipulation, freeOffset);
        };
    }

    private static String memberKey(ByteCodeElement.Member original) {
        return original.getDeclaringType().asErasure().getName() + "#" + original.getInternalName() + original.getDescriptor();
    }
}
