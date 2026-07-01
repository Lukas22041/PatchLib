package patchlib.agent.context;

import patchlib.api.query.FieldQuery;
import patchlib.api.query.MethodQuery;
import patchlib.api.ref.FieldRef;
import patchlib.api.ref.MethodRef;
import patchlib.api.ref.Ref;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/** Provides reflection utilities for patches. Loaded on the system classloader, so it is not affected by the reflection block.
Caches results based on the query used to discover them, which means that only the first call needs to do the full search on a class for the members.*/
final class PatchReflection {

    private PatchReflection() { }

    private static final MethodHandles.Lookup LOOKUP = MethodHandles.lookup();

    private static final ConcurrentHashMap<Object, Optional<Field>> FIELD_CACHE = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<Object, Optional<Method>> METHOD_CACHE = new ConcurrentHashMap<>();

    static <T> Ref<T> field(Class<?> owner, Object target, FieldQuery query) {
        Field field = FIELD_CACHE.computeIfAbsent(query.cacheKey(owner), key -> Optional.ofNullable(resolveField(owner, query)))
                .orElseThrow(() -> new RuntimeException("No field matching the query on " + owner.getName() + " or its supertypes"));
        return new FieldRef<>(field, target);
    }

    static MethodRef method(Class<?> owner, Object target, MethodQuery query) {
        Method method = METHOD_CACHE.computeIfAbsent(query.cacheKey(owner), key -> Optional.ofNullable(resolveMethod(owner, query)))
                .orElseThrow(() -> new RuntimeException("No method matching the query on " + owner.getName() + " or its supertypes"));
        return new MethodRef(bind(method, target));
    }

    static boolean hasField(Class<?> owner, FieldQuery query) {
        return FIELD_CACHE.computeIfAbsent(query.cacheKey(owner), key -> Optional.ofNullable(resolveField(owner, query))).isPresent();
    }

    static boolean hasMethod(Class<?> owner, MethodQuery query) {
        return METHOD_CACHE.computeIfAbsent(query.cacheKey(owner), key -> Optional.ofNullable(resolveMethod(owner, query))).isPresent();
    }

    private static Field resolveField(Class<?> owner, FieldQuery query) {
        for (Class<?> clazz = owner; clazz != null; clazz = clazz.getSuperclass())
            for (Field field : clazz.getDeclaredFields())
                if (query.matches(field)) {
                    field.setAccessible(true);
                    return field;
                }
        return null;
    }

    private static Method resolveMethod(Class<?> owner, MethodQuery query) {
        for (Class<?> clazz = owner; clazz != null; clazz = clazz.getSuperclass())
            for (Method method : clazz.getDeclaredMethods())
                if (query.matches(method)) {
                    method.setAccessible(true);
                    return method;
                }
        return null;
    }

    static MethodHandle bind(Method m, Object target) {
        try {
            MethodHandle handle = LOOKUP.unreflect(m);
            return Modifier.isStatic(m.getModifiers()) || target == null ? handle : handle.bindTo(target);
        } catch (IllegalAccessException e) {
            throw new RuntimeException("Could not access method " + m, e);
        }
    }
}
