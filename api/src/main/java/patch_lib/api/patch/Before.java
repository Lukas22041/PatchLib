package patch_lib.api.patch;

import patch_lib.api.match.MethodType;
import patch_lib.api.match.Unset;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** Patches a method for before it has executed */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.CLASS)
public @interface Before {
    String methodName() default "";

    Class<?>[] parameters() default {};
    /**Uses actual class names, with type erasure.
     * Examples: "int", "int[]", "java.lang.Integer", "java.lang.Integer[]", "java.util.List", "void" */
    String[] parameterNames() default {};
    int parameterCount() default -1;

    Class<?> returnType() default Unset.class;
    /**Uses actual class names, with type erasure.
     * Examples: "int", "int[]", "java.lang.Integer", "java.lang.Integer[]", "java.util.List", "void" */
    String returnTypeName() default "";

    /** Order in which patches are executed, lower numbers are run first.
    Two patches with the same priority are ordered based on mod name */
    int priority() default 0;

    MethodType methodType() default MethodType.ANY;
    boolean staticOnly() default false;

}
