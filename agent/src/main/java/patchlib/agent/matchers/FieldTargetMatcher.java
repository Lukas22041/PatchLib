package patchlib.agent.matchers;

import net.bytebuddy.description.field.FieldDescription;
import net.bytebuddy.matcher.ElementMatcher;
import patchlib.agent.spec.RedirectSiteSpec;
import patchlib.agent.spec.TargetFieldSpec;

import static net.bytebuddy.matcher.ElementMatchers.*;

public class FieldTargetMatcher {

    public static ElementMatcher.Junction<FieldDescription> create(TargetFieldSpec spec) {
        return refine(any(), spec.fieldName(), spec.fieldType(), spec.fieldSubtype(), spec.staticOnly());
    }

    /** Matches the field access a @RedirectFieldRead/@RedirectFieldWrite intercepts inside the host body. Same shape
     * matching as a target field, plus the owner, which is the class declaring the field. Read vs write is decided by
     * the installer, not here. */
    public static ElementMatcher.Junction<FieldDescription> create(RedirectSiteSpec spec) {
        ElementMatcher.Junction<FieldDescription> base = any();

        if (!spec.owner().matchesEverything())
            base = base.and(isDeclaredBy(ClassTargetMatcher.create(spec.owner())));

        return refine(base, spec.name(), spec.returnOrFieldType(), spec.fieldSubtype(), spec.staticOnly());
    }

    /** Adds the name/type/subtype/static constraints shared by target fields and redirected accesses onto a base matcher. */
    private static ElementMatcher.Junction<FieldDescription> refine(ElementMatcher.Junction<FieldDescription> matcher,
                                                                    String name, String type, String subtype, boolean staticOnly) {
        if (!name.isEmpty())
            matcher = matcher.and(named(name));

        if (!type.isEmpty())
            matcher = matcher.and(fieldType(named(type)));

        if (!subtype.isEmpty())
            matcher = matcher.and(fieldType(hasSuperType(named(subtype))));

        if (staticOnly)
            matcher = matcher.and(isStatic());

        return matcher;
    }
}
