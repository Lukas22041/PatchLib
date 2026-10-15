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

    /** Can be used to filter to a specific package, like "com.fs.starfarer.api". Only matches classes directly in it, unless packageRecursive is set. */
    String packageName() default "";
    /** Makes packageName recursive, also matching deeper subpackages of it. */
    boolean packageRecursive() default false;

    /** Can be used to exclude a specific package. Only excludes classes directly in it, unless excludedPackageRecursive is set.
     * For example, "com.fs" with excludedPackageRecursive = true excludes all of the game's own classes, so only modded classes are patched. */
    String excludedPackageName() default "";
    /** Makes excludedPackageName recursive, also excluding deeper subpackages of it. */
    boolean excludedPackageRecursive() default false;

    /** Match based on the shape of methods within the class */
    MethodMatch[] methodMatches() default {};

    /** Match based on the shape of fields within the class */
    FieldMatch[] fieldMatches() default {};

    /** Match classes annotated with all of these. Only the annotation's presence is checked, not its values;
     * read the values from inside the patch if you need them. The annotation must have CLASS or RUNTIME retention. */
    Class<?>[] annotations() default {};
    /** Uses actual class names, with type erasure. Only the annotation's presence is checked, not its values. */
    String[] annotationNames() default {};
}
