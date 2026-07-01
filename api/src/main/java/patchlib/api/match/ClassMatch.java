package patchlib.api.match;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/** Identifies classes by name, supertype, package, or the shape of their members. Used by @Patch to pick the classes
 * to patch, and by the redirect annotations to constrain the owner of an intercepted call or field access.
 * Every non-default filter used is AND-ed together; a fully default ClassMatch matches every class. */
@Retention(RetentionPolicy.CLASS)
public @interface ClassMatch {

    Class<?> type() default Unset.class;
    /**Uses actual class names, with type erasure.
     * Examples: "java.util.List", "com.fs.starfarer.api.campaign.CampaignClockAPI" */
    String typeName() default "";

    Class<?> subtype() default Unset.class;
    /**Uses actual class names, with type erasure.
     * Examples: "java.util.List", "com.fs.starfarer.api.campaign.CampaignClockAPI" */
    String subtypeName() default "";

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
