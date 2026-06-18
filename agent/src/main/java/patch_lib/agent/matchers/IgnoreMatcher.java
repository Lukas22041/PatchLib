package patch_lib.agent.matchers;

import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.matcher.ElementMatcher;

import static net.bytebuddy.matcher.ElementMatchers.*;

/** Prevents core JVM classes from being targeted by patches. Mostly a performance consideration, but also for stability.
 * Also prevents patches to bytebuddy and patchlib itself*/
public class IgnoreMatcher {

    public static ElementMatcher.Junction<TypeDescription> create() {
        return nameStartsWith("java.")
                .or(nameStartsWith("javax."))
                .or(nameStartsWith("jdk."))
                .or(nameStartsWith("sun."))
                .or(nameStartsWith("com.sun."))
                .or(nameStartsWith("patch_lib.agent."))
                .or(nameStartsWith("patch_lib.bytebuddy."))
                .or(nameStartsWith("patch_lib.api."));
    }

}
