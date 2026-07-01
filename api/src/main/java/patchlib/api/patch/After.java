package patchlib.api.patch;

import patchlib.api.match.MethodMatch;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;


/** Patches a method for after it has executed */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.CLASS)
public @interface After {

    /** The method to patch. The default matches every method of the patched class. */
    MethodMatch target() default @MethodMatch;

    /** Order in which patches are executed, lower numbers are run first.
    Two patches with the same priority are ordered based on mod name */
    int priority() default 0;

}
