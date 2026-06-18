package patch_lib.api;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** Annotation to mark a class as a patch. Without it, other annotations are ignored.
 * By default, a patch without any filter set matches ALL classes. You should always filter to as few potential targets as possible.
 * Every non-default filter used is AND-ed together. */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.CLASS)
public @interface Patch {

    //Either provide a class reference or the whole class name with package
    Class<?> targetClass() default Unset.class;
    String targetClassName() default "";

    Class<?> targetSubtype() default Unset.class;
    String targetSubtypeName() default "";

    String targetPackage() default "";
    boolean includeSubpackages() default false;

    MethodMatch[] methodMatches() default {};
}
