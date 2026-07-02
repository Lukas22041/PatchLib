package patchlib.api.patch;

import patchlib.api.match.ClassMatch;
import patchlib.api.match.MethodMatch;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** Redirects a constructor call inside a method. The target picks the host method to instrument, and type and
 * constructor pick the new expression within it to intercept. A handler can run code around the construction,
 * change its arguments, or supply a different instance entirely.
 * When several mods redirect the same construction they nest as layers, lowest priority outermost. Each layer calls
 * ctx.call() to reach the next layer down (eventually the real construction), or never calls it to short circuit.
 * In a constructor host, new expressions of the host class or its direct superclass are never intercepted. */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.CLASS)
public @interface RedirectNew {

    /** The host method whose body is searched for the construction to intercept. The default searches every method of the patched class. */
    MethodMatch target() default @MethodMatch;

    /** The class being instantiated. The default intercepts constructions of every class. */
    ClassMatch type() default @ClassMatch;

    /** The constructor to intercept. Only the parameter members are used, a constructor has no name or return type. */
    MethodMatch constructor() default @MethodMatch;

    /** Order in which layers are applied, lower numbers are the outermost layer and run first.
    Two redirects with the same priority are ordered based on mod name */
    int priority() default 0;
}
