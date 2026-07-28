package patchlib.agent.context.util;

import net.bytebuddy.description.field.FieldDescription;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.matcher.ElementMatcher;
import patchlib.agent.matchers.FieldMatcher;
import patchlib.agent.matchers.MethodMatcher;
import patchlib.api.ref.FieldRef;
import patchlib.api.ref.MethodRef;
import patchlib.api.ref.Ref;
import patchlib.api.spec.FieldQuerySpec;
import patchlib.api.spec.MethodQuerySpec;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class PatchReflection {

    private PatchReflection() { }

    private static final MethodHandles.Lookup LOOKUP = MethodHandles.lookup();

    private record MethodCacheKey(Class<?> owner, MethodQuerySpec querySpec) { }
    private record FieldCacheKey(Class<?> owner, FieldQuerySpec querySpec) { }

    private record CachedMethod(Method method, MethodHandle handle, MethodHandle spreadHandle) { }

    private static final ConcurrentHashMap<MethodCacheKey, Optional<CachedMethod>> METHOD_CACHE = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<FieldCacheKey, Optional<Field>> FIELD_CACHE = new ConcurrentHashMap<>();

    public static MethodRef getMethod(Class<?> owner, Object instance, MethodQuerySpec query) {

        MethodCacheKey key = new MethodCacheKey(owner, query);

        CachedMethod cached = METHOD_CACHE.computeIfAbsent(key, cacheKey -> {
            Method method = findMethod(cacheKey.owner, cacheKey.querySpec);
            if (method == null) return Optional.empty();
            return Optional.of(createCachedMethod(method));
        }).orElseThrow(() -> new RuntimeException("No method matching the query on " + owner.getName() + " with the query " + query));

        boolean isStatic = Modifier.isStatic(cached.method.getModifiers());
        //Static methods have no instance
        Object receiver = isStatic || instance == null ? null : instance;

        return new MethodRef(cached.handle, cached.spreadHandle, receiver);
    }

    public static <T> Ref<T> getField(Class<?> owner, Object instance, FieldQuerySpec query) {

        FieldCacheKey key = new FieldCacheKey(owner, query);

        Field field = FIELD_CACHE.computeIfAbsent(key, cacheKey -> {
            return Optional.ofNullable(findField(cacheKey.owner, cacheKey.querySpec));
        }).orElseThrow(() -> new RuntimeException("No field matching the query on " + owner.getName() + " with the query " + query));

        return new FieldRef<>(field, instance);
    }

    public static boolean hasMethod(Class<?> owner, Object instance, MethodQuerySpec query) {
        MethodCacheKey key = new MethodCacheKey(owner, query);
        return METHOD_CACHE.computeIfAbsent(key, cacheKey -> {
            Method method = findMethod(cacheKey.owner, cacheKey.querySpec);
            if (method == null) return Optional.empty();
            return Optional.of(createCachedMethod(method));
        }).isPresent();
    }

    public static boolean hasField(Class<?> owner, Object instance, FieldQuerySpec query) {
        FieldCacheKey key = new FieldCacheKey(owner, query);
        return FIELD_CACHE.computeIfAbsent(key, cacheKey -> Optional.ofNullable(findField(cacheKey.owner, cacheKey.querySpec))).isPresent();
    }

    private static Method findMethod(Class<?> owner, MethodQuerySpec query) {
        ElementMatcher.Junction<MethodDescription> matcher = MethodMatcher.fromQuery(query);

        Class<?> current = owner;

        //Iterate over each superclass to find declared methods
        while (current != null) {
            for (Method method : current.getDeclaredMethods()) {
                MethodDescription.InDefinedShape methodDescription = new MethodDescription.ForLoadedMethod(method);
                if (matcher.matches(methodDescription)) {
                    method.setAccessible(true);
                    return method;
                }
            }

            current = current.getSuperclass();
        }

        return null;
    }

    private static Field findField(Class<?> owner, FieldQuerySpec query) {
        ElementMatcher.Junction<FieldDescription> matcher = FieldMatcher.fromQuery(query);

        Class<?> current = owner;

        //Iterate over each superclass to find declared methods
        while (current != null) {
            for (Field field : current.getDeclaredFields()) {
                FieldDescription.InDefinedShape fieldDescription = new FieldDescription.ForLoadedField(field);
                if (matcher.matches(fieldDescription)) {
                    field.setAccessible(true);
                    return field;
                }
            }

            current = current.getSuperclass();
        }

        return null;
    }

    private static CachedMethod createCachedMethod(Method method) {
        try {
            MethodHandle handle = LOOKUP.unreflect(method);
            int parameterCount = handle.type().parameterCount();
            MethodType type = MethodType.genericMethodType(parameterCount);

            //Used to be able to call the handle with an array of objects in the MethodRef
            MethodHandle spreadHandle = handle.asType(type).asSpreader(Object[].class, parameterCount);

            return new CachedMethod(method, handle, spreadHandle);
        } catch (IllegalAccessException ex) {
            throw new RuntimeException("Could not access method " + method, ex);
        }
    }

}
