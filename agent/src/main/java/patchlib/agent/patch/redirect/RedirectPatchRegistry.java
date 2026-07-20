package patchlib.agent.patch.redirect;

import net.bytebuddy.description.ByteCodeElement;
import net.bytebuddy.description.method.MethodDescription;

public class RedirectPatchRegistry {



    public static String getSiteKey(ByteCodeElement.Member method, String kind, ByteCodeElement.Member member) {
        return getMemberKey(method) + " - " + kind + " - " + getMemberKey(member);
    }

    public static String getMemberKey(ByteCodeElement.Member member) {
        return member.getDeclaringType().asErasure().getName() + ":" + member.getInternalName() + ":" + member.getDescriptor();
    }

}
