package patchlib.agent.patch.redirect;

import net.bytebuddy.asm.MemberSubstitution;
import net.bytebuddy.description.ByteCodeElement;
import net.bytebuddy.description.field.FieldDescription;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.description.type.TypeList;
import net.bytebuddy.implementation.bytecode.StackManipulation;
import net.bytebuddy.implementation.bytecode.assign.Assigner;
import net.bytebuddy.matcher.ElementMatcher;
import net.bytebuddy.pool.TypePool;
import net.bytebuddy.utility.JavaConstant;
import patchlib.agent.patch.InstallationData;
import patchlib.agent.spec.PatchHandlerSpec;

import java.lang.reflect.Method;
import java.util.List;

public class RedirectSubstitutionFactory implements MemberSubstitution.Substitution.Factory<MemberSubstitution.Target.ForMember> {

    protected record RedirectCandidate(ElementMatcher<ByteCodeElement.Member> matcher, InstallationData data) { }

    private final PatchHandlerSpec.RedirectType redirectType;
    private final String hostKey;
    private final TypeDescription typeDescription;
    private final List<RedirectCandidate> candidates;

    public RedirectSubstitutionFactory(PatchHandlerSpec.RedirectType redirectType, String hostKey, TypeDescription typeDescription, List<RedirectCandidate> candidates) {
        this.redirectType = redirectType;
        this.hostKey = hostKey;
        this.typeDescription = typeDescription;
        this.candidates = candidates;
    }

    /**
     * Creates a substitution for an instrumented method.
     *
     * @param instrumentedType   The instrumented type.
     * @param instrumentedMethod The instrumented method.
     * @param typePool           The type pool being used.
     * @return The substitution to apply within the instrumented method.
     */
    @Override
    public MemberSubstitution.Substitution<? super MemberSubstitution.Target.ForMember> make(TypeDescription instrumentedType, MethodDescription instrumentedMethod, TypePool typePool) {
        return new SiteSubstitution(instrumentedType, instrumentedMethod, typePool);
    }

    private final class SiteSubstitution implements MemberSubstitution.Substitution<MemberSubstitution.Target.ForMember> {

        private final TypeDescription instrumentedType;
        private final MethodDescription instrumentedMethod;
        private final TypePool typePool;

        private SiteSubstitution(TypeDescription instrumentedType, MethodDescription instrumentedMethod, TypePool typePool) {
            this.instrumentedType = instrumentedType;
            this.instrumentedMethod = instrumentedMethod;
            this.typePool = typePool;
        }

        /**
         * Resolves this substitution into a stack manipulation.
         *
         * @param target            The targeted member that is substituted.
         * @param parameters        All parameters that serve as input to this access.
         * @param result            The result that is expected from the interaction or {@code void} if no result is expected.
         * @param methodHandle      A method handle describing the substituted expression.
         * @param stackManipulation The original byte code expression that is being executed.
         * @param freeOffset        The first free offset of the local variable array that can be used for storing values.
         * @return A stack manipulation that represents the access.
         */
        @Override
        public StackManipulation resolve(MemberSubstitution.Target.ForMember target, TypeList.Generic parameters, TypeDescription.Generic result, JavaConstant.MethodHandle methodHandle, StackManipulation stackManipulation, int freeOffset) {
            //The original bytecode this will replace
            ByteCodeElement.Member original = target.getMember();

            List<InstallationData> matched = getMatchedCandidates(original);

            //Return the original bytecode if there are no patches
            if (matched.isEmpty()) {
                if (redirectType != PatchHandlerSpec.RedirectType.CONSTRUCTOR) return stackManipulation;

                //Constructors are handled a bit differently, so this uses bytebuddy's member substitution to recreate the original bytecode
                return MemberSubstitution.Substitution.Chain.with(Assigner.DEFAULT, Assigner.Typing.DYNAMIC)
                        .executing(Chain.Step.OfOriginalExpression.INSTANCE)
                        .make(instrumentedType, instrumentedMethod, typePool)
                        .resolve(target, parameters, result, methodHandle, stackManipulation, freeOffset);
            }

            //Create the redirect site & register it. Re-registers will return the existing patch-site
            //Takes in the redirect type as otherwise field read & write may get the same site key.
            String siteKey = RedirectPatchRegistry.getSiteKey(hostKey, redirectType.toString(), original);
            RedirectPatchSite site = createRedirectSite(matched);
            int id = RedirectPatchRegistry.register(siteKey, site);

            Method delegate = getDelegate(original);
            //The constant dynamic requires a constant-pool valid entry, and booleans aren't a real type there and are just represented as integers.
            int hasReceiver = hasReceiver(original) ? 1 : 0;

            //ForDelegation replaces the original bytecode with an invokestatic member that calls the delegate
            return MemberSubstitution.Substitution.Chain.with(Assigner.DEFAULT, Assigner.Typing.DYNAMIC)
                    .executing(MemberSubstitution.Substitution.Chain.Step.ForDelegation
                            .withCustomMapping()
                            .bind(RedirectHandleMarker.class, JavaConstant.Dynamic.bootstrap())
                            .to(delegate))
                    .make(instrumentedType, instrumentedMethod, typePool)
                    .resolve(target, parameters, result, methodHandle, stackManipulation, freeOffset);

        }

        private Method getDelegate(ByteCodeElement.Member original) {
            if (redirectType.equals(PatchHandlerSpec.RedirectType.METHOD_CALL) && original instanceof MethodDescription methodDescription) {
                if (methodDescription.isStatic()) {
                    return ;
                } else {
                    return ;
                }
            }
            else if (redirectType.equals(PatchHandlerSpec.RedirectType.CONSTRUCTOR)) return ;
            else if (redirectType.equals(PatchHandlerSpec.RedirectType.FIELD_READ)) return ;
            else if (redirectType.equals(PatchHandlerSpec.RedirectType.FIELD_WRITE)) return ;
            throw new IllegalArgumentException("Called getBridge with unknown redirect type");
        }

        /** Checks if the replaced element has a receiver, needed for the constant dynamic bootstrap */
        private boolean hasReceiver(ByteCodeElement.Member original) {
            if (redirectType.equals(PatchHandlerSpec.RedirectType.METHOD_CALL)) return original instanceof MethodDescription methodDescription && !methodDescription.isStatic();
            else if (redirectType.equals(PatchHandlerSpec.RedirectType.CONSTRUCTOR)) return false;
            else if (redirectType.equals(PatchHandlerSpec.RedirectType.FIELD_READ) || redirectType.equals(PatchHandlerSpec.RedirectType.FIELD_WRITE))
                return original instanceof FieldDescription fieldDescription && !fieldDescription.isStatic();
            throw new IllegalArgumentException("Called hasReceiver with unknown redirect type");
        }

        /**The redirect installer passes all matchers of a redirect type that match to the target host method down here,
         * so this filters it down to the exact match for specific elements. The sorting already happened in PatchInstaller. */
        private List<InstallationData> getMatchedCandidates(ByteCodeElement.Member original) {
            return candidates.stream().filter(candidate -> candidate.matcher.matches(original)).map(candidate -> candidate.data).toList();
        }

        private RedirectPatchSite createRedirectSite(List<InstallationData> matched) {
            return new RedirectPatchSite(matched, redirectType);
        }
    }
}
