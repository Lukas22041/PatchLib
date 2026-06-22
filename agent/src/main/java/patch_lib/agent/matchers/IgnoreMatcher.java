package patch_lib.agent.matchers;

import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.matcher.ElementMatcher;

import static net.bytebuddy.matcher.ElementMatchers.*;

/** Prevents core JVM classes from being targeted by patches. Mostly a performance consideration, but also for stability.
 * Also prevents patches to bytebuddy and patchlib itself*/
public class IgnoreMatcher {

    private static final String[] IGNORED_PREFIXES = {
            "java.", "javax.", "jdk.", "sun.", "com.sun.", //Ignore core JVM classes
            "kotlin.", "kotlinx.", //Ignore the kotlin runtime
            "patch_lib.", //Ignore PatchLib itself and its bundled Bytebuddy
            "org.apache.log4j.", //Ignore the logger
            "com.azul.", "org.graalvm", "com.oracle.", "oracle.", //Ignore specific JVMs
            "org.codehaus.janino.", "org.codehaus.commons.", //Ignore Janino
            "com.intellij.", "org.jetbrains.capture." //Ignore Intellijs debugger
    };

    public static ElementMatcher.Junction<TypeDescription> create() {
        ElementMatcher.Junction<TypeDescription> matcher = none();
        for (String prefix : IGNORED_PREFIXES) matcher = matcher.or(nameStartsWith(prefix));
        return matcher;
    }

    public static boolean isIgnored(String binaryName) {
        for (String prefix : IGNORED_PREFIXES) if (binaryName.startsWith(prefix)) return true;
        return false;
    }

}
