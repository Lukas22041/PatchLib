package patchlib.api.patch;

import patchlib.api.match.ClassMatch;
import patchlib.api.match.FieldMatch;
import patchlib.api.match.MethodMatch;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** Redirects a field read inside a method. The target picks the host method to instrument, and field picks the
 * field whose read to intercept. A handler can pass the read value through, transform it, or replace it entirely.
 * When several mods redirect the same read they nest as layers, lowest priority outermost. Each layer calls ctx.read()
 * to reach the next layer down (eventually the real read), or never calls it to short circuit. */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.CLASS)
public @interface RedirectFieldRead {

    /** The host method whose body is searched for the read to intercept. The default searches every method of the patched class. */
    MethodMatch target() default @MethodMatch;

    /** The field whose read to intercept inside the host method. The default intercepts every field read in the host body. */
    FieldMatch field() default @FieldMatch;

    /** The class declaring the field. The default applies no owner constraint. */
    ClassMatch owner() default @ClassMatch;

    /** Order in which layers are applied, lower numbers are the outermost layer and run first.
    Two redirects with the same priority are ordered based on mod name */
    int priority() default 0;
}
