package patch_lib.api.query;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

/** Matcher builder for finding local fields on a patched class. A default/null value is not considered in the matching selection.
 * The first matching method wins. */
public final class FieldQuery {

    String name;
    Class<?> type;
    Class<?> subtypeOf;
    Boolean staticOnly;

    private FieldQuery() { }

    public static FieldQuery any() { return new FieldQuery(); }
    public static FieldQuery named(String name) { return new FieldQuery().name(name); }

    public FieldQuery name(String name) { this.name = name; return this; }
    public FieldQuery type(Class<?> type) { this.type = type; return this; }
    public FieldQuery subtypeOf(Class<?> type) { this.subtypeOf = type; return this; }
    public FieldQuery staticOnly(boolean staticOnly) { this.staticOnly = staticOnly; return this; }

    public boolean matches(Field f) {
        if (name != null && !f.getName().equals(name)) return false;
        if (type != null && !f.getType().equals(type)) return false;
        if (subtypeOf != null && !subtypeOf.isAssignableFrom(f.getType())) return false;
        if (staticOnly != null && staticOnly != Modifier.isStatic(f.getModifiers())) return false;
        return true;
    }

    /** Builds a key of the query, relevant for caching the search result later */
    public Object cacheKey(Class<?> owner) {
        return new Key(owner, name, type, subtypeOf, staticOnly);
    }

    private record Key(Class<?> owner, String name, Class<?> type, Class<?> subtypeOf, Boolean staticOnly) { }
}
