package patch_lib.api.query;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

/** Matcher builder for finding local methods on a patched class. A default/null value is not considered in the matching selection.
 * The first matching method wins. */
public final class MethodQuery {

    String name;
    Class<?>[] parameterTypes;
    int parameterCount = -1;
    Class<?> returnType;
    Boolean staticOnly;

    private MethodQuery() {
    }

    public static MethodQuery any() {
        return new MethodQuery();
    }

    public static MethodQuery named(String name) {
        return new MethodQuery().name(name);
    }

    public MethodQuery name(String name) {
        this.name = name;
        return this;
    }

    public MethodQuery params(Class<?>... parameterTypes) {
        this.parameterTypes = parameterTypes;
        return this;
    }

    public MethodQuery paramCount(int count) {
        this.parameterCount = count;
        return this;
    }

    public MethodQuery returns(Class<?> returnType) {
        this.returnType = returnType;
        return this;
    }

    public MethodQuery staticOnly(boolean staticOnly) {
        this.staticOnly = staticOnly;
        return this;
    }


    public boolean matches(Method m) {
        if (name != null && !m.getName().equals(name)) return false;
        if (returnType != null && !m.getReturnType().equals(returnType)) return false;
        if (staticOnly != null && staticOnly != Modifier.isStatic(m.getModifiers())) return false;
        if (parameterTypes != null) {
            if (m.getParameterCount() != parameterTypes.length) return false;
            Class<?>[] actual = m.getParameterTypes();
            for (int i = 0; i < actual.length; i++)
                if (!actual[i].equals(parameterTypes[i])) return false;
        } else if (parameterCount >= 0 && m.getParameterCount() != parameterCount) {
            return false;
        }
        return true;
    }
}
