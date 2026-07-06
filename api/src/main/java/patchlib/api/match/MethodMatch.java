package patchlib.api.match;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.CLASS)
public @interface MethodMatch {
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

    MethodType methodType() default MethodType.ANY;
    boolean staticOnly() default false;

    /** Match methods annotated with all of these. Only the annotation's presence is checked, not its values;
     * read the values from inside the patch if you need them. The annotation must have CLASS or RUNTIME retention. */
    Class<?>[] annotations() default {};
    /** Uses actual class names, with type erasure. Only the annotation's presence is checked, not its values. */
    String[] annotationNames() default {};

}
