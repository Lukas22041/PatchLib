package patchlib.api.patch;

import patchlib.api.match.ClassMatch;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** Annotation to mark a class as a patch. Without it, other annotations are ignored.
 * The target picks the classes to patch. By default, a patch without any filter set matches all classes.
 * You should always filter to as few potential targets as possible. */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.CLASS)
public @interface Patch {

    /** The classes to patch. The default matches every class. */
    ClassMatch target() default @ClassMatch;
}
