package patch_lib.api.query;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** Matcher builder for finding local methods on a patched class. A default/null value is not considered in the matching selection.
 * The first matching method wins. */
public final class MethodQuery {

    String name;
    List<Class<?>> parameterTypes;
    List<Class<?>> paramsContain;
    int parameterCount = -1;
    Class<?> returnType;
    Class<?> returnsSubtypeOf;
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

    /** Exact parameter matching */
    public MethodQuery params(Class<?>... parameterTypes) {
        this.parameterTypes = List.of(parameterTypes);
        return this;
    }

    /** Lose parameter matching */
    public MethodQuery paramsContain(Class<?>... types) {
        this.paramsContain = List.of(types);
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

    public MethodQuery returnsSubtypeOf(Class<?> returnType) {
        this.returnsSubtypeOf = returnType;
        return this;
    }

    public MethodQuery staticOnly(boolean staticOnly) {
        this.staticOnly = staticOnly;
        return this;
    }

    public boolean matches(Method m) {
        if (name != null && !m.getName().equals(name)) return false;
        if (returnType != null && !m.getReturnType().equals(returnType)) return false;
        if (returnsSubtypeOf != null && !returnsSubtypeOf.isAssignableFrom(m.getReturnType())) return false;
        if (staticOnly != null && staticOnly != Modifier.isStatic(m.getModifiers())) return false;
        if (parameterTypes != null) {
            if (m.getParameterCount() != parameterTypes.size()) return false;
            Class<?>[] actual = m.getParameterTypes();
            for (int i = 0; i < actual.length; i++)
                if (!actual[i].equals(parameterTypes.get(i))) return false;
        } else if (parameterCount >= 0 && m.getParameterCount() != parameterCount) {
            return false;
        }
        if (paramsContain != null) {
            List<Class<?>> remaining = new ArrayList<>(Arrays.asList(m.getParameterTypes()));
            for (Class<?> required : paramsContain)
                if (!remaining.remove(required)) {
                    return false;
                }
        }
        return true;
    }

    /** Builds a key of the query, relevant for caching the search result later */
    public Object cacheKey(Class<?> owner) {
        return new Key(owner, name, parameterTypes, paramsContain, parameterCount, returnType, returnsSubtypeOf, staticOnly);
    }

    private record Key(Class<?> owner, String name, List<Class<?>> parameterTypes, List<Class<?>> paramsContain,
                       int parameterCount, Class<?> returnType, Class<?> returnsSubtypeOf, Boolean staticOnly) { }
}
