package patchlib.agent.patch.advice;

import net.bytebuddy.description.method.MethodDescription;

public class AdvicePatchRegistry {


    public static String getSiteKey(MethodDescription member) {
        return member.getDeclaringType().asErasure().getName() + ":" + member.getInternalName() + ":" + member.getDescriptor();
    }

}
