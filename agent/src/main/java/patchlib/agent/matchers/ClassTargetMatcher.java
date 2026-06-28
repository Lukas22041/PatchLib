package patchlib.agent.matchers;

import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.matcher.ElementMatcher;
import patchlib.agent.spec.TargetClassSpec;
import patchlib.agent.spec.TargetFieldSpec;
import patchlib.agent.spec.TargetMethodSpec;

import static net.bytebuddy.matcher.ElementMatchers.*;

public class ClassTargetMatcher {
    public static ElementMatcher.Junction<TypeDescription> create(TargetClassSpec classSpec) {

        ElementMatcher.Junction<TypeDescription> matcher = any();

        if (!classSpec.targetClass().isEmpty())
            matcher = matcher.and(named(classSpec.targetClass()));

        if (!classSpec.targetSubtype().isEmpty())
            matcher = matcher.and(hasSuperType(named(classSpec.targetSubtype())));

        String targetPackage = classSpec.targetPackage();
        if (!targetPackage.isEmpty()) {
            //Recursive search
            if (classSpec.includeSubpackages()) {
                matcher = matcher.and((TypeDescription type) -> {
                            if (type.getPackage() == null) return false;
                            String packageName = type.getPackage().getName();
                            return packageName.equals(targetPackage) || packageName.startsWith(targetPackage + ".");
                        });
            } else {
                matcher = matcher.and((TypeDescription type) ->
                    type.getPackage() != null && type.getPackage().getName().equals(targetPackage)
                );
            }
        }

        for (TargetMethodSpec methodSpec : classSpec.methodMatches()) {
            matcher = matcher.and(declaresMethod(MethodTargetMatcher.create(methodSpec)));
        }

        for (TargetFieldSpec fieldSpec : classSpec.fieldMatches()) {
            matcher = matcher.and(declaresField(FieldTargetMatcher.create(fieldSpec)));
        }

        return matcher;
    }
}
