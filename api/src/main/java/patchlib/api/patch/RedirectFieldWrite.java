package patchlib.api.patch;

import patchlib.api.match.ClassMatch;
import patchlib.api.match.FieldMatch;
import patchlib.api.match.MethodMatch;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** Redirects a field write inside a method. The target picks the host method to instrument, and field picks the
 * field whose write to intercept. A handler can pass the written value through, transform it, or drop the write.
 * When several mods redirect the same write they nest as layers, lowest priority outermost. Each layer calls ctx.write()
 * to reach the next layer down (eventually the real write), or never calls it to short circuit. */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.CLASS)
public @interface RedirectFieldWrite {

    /** The host method whose body is searched for the write to intercept. The default searches every method of the patched class. */
    MethodMatch target() default @MethodMatch;

    /** The field whose write to intercept inside the host method. The default intercepts every field write in the host body. */
    FieldMatch field() default @FieldMatch;

    /** The class declaring the field. The default applies no owner constraint. */
    ClassMatch owner() default @ClassMatch;

    /** Order in which layers are applied, lower numbers are the outermost layer and run first.
    Two redirects with the same priority are ordered based on mod name */
    int priority() default 0;
}
