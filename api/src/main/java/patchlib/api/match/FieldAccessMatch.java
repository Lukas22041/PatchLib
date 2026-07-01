package patchlib.api.match;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/** Identifies a field read or write inside a redirected host method. The owner is the class that declares the field. */
@Retention(RetentionPolicy.CLASS)
public @interface FieldAccessMatch {

    Class<?> owner() default Unset.class;
    /**Uses actual class names, with type erasure.
     * Examples: "java.util.List", "com.fs.starfarer.api.campaign.CampaignClockAPI" */
    String ownerName() default "";

    String fieldName() default "";

    Class<?> fieldType() default Unset.class;
    /**Uses actual class names, with type erasure.
     * Examples: "int", "int[]", "java.lang.Integer", "java.lang.Integer[]", "java.util.List" */
    String fieldTypeName() default "";

    Class<?> fieldSubtype() default Unset.class;
    /**Uses actual class names, with type erasure.
     * Examples: "java.util.List", "com.fs.starfarer.api.campaign.CampaignClockAPI" */
    String fieldSubtypeName() default "";

    /** Whether to intercept the read or the write of the field. A read yields a value to transform, a write carries the value being stored. */
    AccessType access() default AccessType.READ;

    boolean staticOnly() default false;
}
