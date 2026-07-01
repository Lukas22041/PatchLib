package patchlib.api.match;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.CLASS)
public @interface FieldMatch {
    String fieldName() default "";

    /** The exact type of the field. */
    Class<?> type() default Unset.class;
    /**Uses actual class names, with type erasure.
     * Examples: "int", "int[]", "java.lang.Integer", "java.lang.Integer[]", "java.util.List" */
    String typeName() default "";

    /** Matches fields whose type is the given type or a subtype of it. */
    Class<?> subtype() default Unset.class;
    /**Uses actual class names, with type erasure.
     * Examples: "java.util.List", "com.fs.starfarer.api.campaign.CampaignClockAPI" */
    String subtypeName() default "";

    boolean staticOnly() default false;
}
