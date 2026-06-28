package patchlib.agent.matchers;

import net.bytebuddy.description.field.FieldDescription;
import net.bytebuddy.matcher.ElementMatcher;
import patchlib.agent.spec.TargetFieldSpec;

import static net.bytebuddy.matcher.ElementMatchers.*;

public class FieldTargetMatcher {

    public static ElementMatcher.Junction<FieldDescription> create(TargetFieldSpec fieldSpec) {

        ElementMatcher.Junction<FieldDescription> matcher = any();

        if (!fieldSpec.fieldName().isEmpty())
            matcher = matcher.and(named(fieldSpec.fieldName()));

        if (!fieldSpec.fieldType().isEmpty())
            matcher = matcher.and(fieldType(named(fieldSpec.fieldType())));

        if (!fieldSpec.fieldSubtype().isEmpty())
            matcher = matcher.and(fieldType(hasSuperType(named(fieldSpec.fieldSubtype()))));

        if (fieldSpec.staticOnly())
            matcher = matcher.and(isStatic());

        return matcher;
    }

}
