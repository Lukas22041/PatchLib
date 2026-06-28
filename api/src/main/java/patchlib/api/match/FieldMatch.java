package patchlib.api.match;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.CLASS)
public @interface FieldMatch {
    String fieldName() default "";

    Class<?> fieldType() default Unset.class;
    /**Uses actual class names, with type erasure.
     * Examples: "int", "int[]", "java.lang.Integer", "java.lang.Integer[]", "java.util.List" */
    String fieldTypeName() default "";

    Class<?> fieldSubtype() default Unset.class;
    /**Uses actual class names, with type erasure.
     * Examples: "java.util.List", "com.fs.starfarer.api.campaign.CampaignClockAPI" */
    String fieldSubtypeName() default "";

    boolean staticOnly() default false;
}
