package patchlib.api.match;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/** Identifies a method call inside a redirected host method. The owner is the class that declares the called method. */
@Retention(RetentionPolicy.CLASS)
public @interface MethodCallMatch {

    Class<?> owner() default Unset.class;
    /**Uses actual class names, with type erasure.
     * Examples: "java.util.List", "com.fs.starfarer.api.campaign.CampaignClockAPI" */
    String ownerName() default "";

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

    boolean staticOnly() default false;
}
