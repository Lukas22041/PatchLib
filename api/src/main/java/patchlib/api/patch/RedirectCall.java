package patchlib.api.patch;

import patchlib.api.match.ClassMatch;
import patchlib.api.match.MethodMatch;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** Redirects a method call inside a method. The target picks the host method to instrument, and call picks the
 * method call within it to intercept. A handler can run code around the call, change its arguments or result,
 * or skip it entirely.
 * When several mods redirect the same call they nest as layers, lowest priority outermost. Each layer calls ctx.call()
 * to reach the next layer down (eventually the real call), or never calls it to short circuit. */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.CLASS)
public @interface RedirectCall {

    /** The host method whose body is searched for the call to intercept. The default searches every method of the patched class. */
    MethodMatch target() default @MethodMatch;

    /** The call to intercept inside the host method. The default intercepts every method call in the host body.
     * methodType is ignored, an intercepted call is never a constructor. */
    MethodMatch call() default @MethodMatch;

    /** The class declaring the called method. The default applies no owner constraint. */
    ClassMatch owner() default @ClassMatch;

    /** Order in which layers are applied, lower numbers are the outermost layer and run first.
    Two redirects with the same priority are ordered based on mod name */
    int priority() default 0;
}
