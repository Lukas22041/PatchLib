package patch_lib.api;

import patch_lib.api.query.FieldQuery;
import patch_lib.api.query.MethodQuery;
import patch_lib.api.ref.ArgRef;
import patch_lib.api.ref.FieldRef;
import patch_lib.api.ref.MethodRef;
import patch_lib.api.ref.Ref;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

final class ClassMembers {

    private ClassMembers() { }

    private static final MethodHandles.Lookup LOOKUP = MethodHandles.lookup();

    public static <T> Ref<T> arg(Object[] args, int index) {
        return new ArgRef<>(args, index);
    }

    public static <T> Ref<T> field(Class<?> owner, Object target, FieldQuery query) {
        for (Class<?> c = owner; c != null; c = c.getSuperclass())
            for (Field f : c.getDeclaredFields())
                if (query.matches(f)) {
                    f.setAccessible(true);
                    return new FieldRef<>(f, target);
                }
        throw new RuntimeException("No field matching the query on " + owner.getName() + " or its supertypes");
    }

    public static MethodRef method(Class<?> owner, Object target, MethodQuery query) {
        for (Class<?> c = owner; c != null; c = c.getSuperclass())
            for (Method m : c.getDeclaredMethods())
                if (query.matches(m)) {
                    m.setAccessible(true);
                    return new MethodRef(bind(m, target));
                }
        throw new RuntimeException("No method matching the query on " + owner.getName() + " or its supertypes");
    }

    private static MethodHandle bind(Method m, Object target) {
        try {
            MethodHandle handle = LOOKUP.unreflect(m);
            return Modifier.isStatic(m.getModifiers()) || target == null ? handle : handle.bindTo(target);
        } catch (IllegalAccessException e) {
            throw new RuntimeException("Could not access method " + m, e);
        }
    }
}