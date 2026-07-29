package patchlib.agent.patch.redirect;

import net.bytebuddy.asm.MemberSubstitution;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.description.type.TypeList;
import net.bytebuddy.implementation.bytecode.StackManipulation;
import net.bytebuddy.pool.TypePool;
import net.bytebuddy.utility.JavaConstant;

public class RedirectSubstitutionFactory implements MemberSubstitution.Substitution.Factory<MemberSubstitution.Target.ForMember> {

    protected record RedirectCandidate(/**TODO This replaces the "Layer" thing from the original code, just a better name*/) { }

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

    private static final class SiteSubstitution implements MemberSubstitution.Substitution<MemberSubstitution.Target.ForMember> {

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
            return null;
        }
    }
}
