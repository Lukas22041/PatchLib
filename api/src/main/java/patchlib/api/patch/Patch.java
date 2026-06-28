package patchlib.api.patch;

import patchlib.api.match.FieldMatch;
import patchlib.api.match.MethodMatch;
import patchlib.api.match.Unset;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** Annotation to mark a class as a patch. Without it, other annotations are ignored.
 * By default, a patch without any filter set matches all classes. You should always filter to as few potential targets as possible.
 * Every non-default filter used is AND-ed together. */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.CLASS)
public @interface Patch {

    Class<?> targetClass() default Unset.class;
    String targetClassName() default "";

    Class<?> targetSubtype() default Unset.class;
    String targetSubtypeName() default "";

    /** Can be used to filter to a specific package, like "com.fs". */
    String targetPackage() default "";
    /** Makes targetPackage recursive, allowing deeper subfolders of the target package */
    boolean includeSubpackages() default false;

    /** Can be used to exclude a specific package, like "com.fs" to only patch modded classes. */
    String excludePackage() default "";
    /** Makes excludePackage recursive, also excluding deeper subfolders of the excluded package */
    boolean excludeSubpackages() default false;

    /** Match based on the shape of methods within the class */
    MethodMatch[] methodMatches() default {};

    /** Match based on the shape of fields within the class */
    FieldMatch[] fieldMatches() default {};
}
