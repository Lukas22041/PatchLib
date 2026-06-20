package patch_lib.api.patch;

import patch_lib.api.match.MethodMatch;
import patch_lib.api.match.Unset;

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

    /** Match based on the shape of methods within the class */
    MethodMatch[] methodMatches() default {};
}
