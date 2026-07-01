package patchlib.api.patch;

import patchlib.api.match.FieldAccessMatch;
import patchlib.api.match.MethodCallMatch;
import patchlib.api.match.MethodMatch;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** Redirects a single call inside a method. The target picks the host method to instrument, and exactly one of
 * methodCall or fieldAccess picks the call or field access within it to intercept. A handler can run code around the
 * call, change its arguments or result, or skip it entirely.
 * When several mods redirect the same call they nest as layers, lowest priority outermost. Each layer calls ctx.call()
 * to reach the next layer down (eventually the real call), or never calls it to short circuit. */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.CLASS)
public @interface Redirect {

    /** The host method whose body is searched for the call to intercept. */
    MethodMatch target();

    /** Intercept a method call inside the host method. Set this or fieldAccess, not both. An empty @MethodCallMatch
     * intercepts every method call in the host body. Modelled as a single-element array only so that "present" is
     * distinguishable from "absent"; write it as methodCall = @MethodCallMatch(...), not as a list. */
    MethodCallMatch[] methodCall() default {};

    /** Intercept a field read or write inside the host method. Set this or methodCall, not both. An empty
     * @FieldAccessMatch intercepts every field access of the chosen kind. Modelled as a single-element array only so
     * that "present" is distinguishable from "absent"; write it as fieldAccess = @FieldAccessMatch(...), not as a list. */
    FieldAccessMatch[] fieldAccess() default {};

    /** Order in which layers are applied, lower numbers are the outermost layer and run first.
    Two redirects with the same priority are ordered based on mod name */
    int priority() default 0;
}
