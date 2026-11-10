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

    private record CachedMethod(Method method, MethodHandle handle, MethodHandle spreadHandle) { }

    //One map per class, keyed by the query itself, so that a lookup does not need to create a key object
    private static final ClassValue<ConcurrentHashMap<MethodQuerySpec, Optional<CachedMethod>>> METHOD_CACHE = new ClassValue<>() {
        @Override
        protected ConcurrentHashMap<MethodQuerySpec, Optional<CachedMethod>> computeValue(Class<?> type) {
            return new ConcurrentHashMap<>();
        }
    };
    private static final ClassValue<ConcurrentHashMap<FieldQuerySpec, Optional<Field>>> FIELD_CACHE = new ClassValue<>() {
        @Override
        protected ConcurrentHashMap<FieldQuerySpec, Optional<Field>> computeValue(Class<?> type) {
            return new ConcurrentHashMap<>();
        }
    };

    public static MethodRef getMethod(Class<?> owner, Object instance, MethodQuerySpec query) {

        Optional<CachedMethod> found = lookupMethod(owner, query);
        if (found.isEmpty()) throw new RuntimeException("No method matching the query on " + owner.getName() + " with the query " + query);
        CachedMethod cached = found.get();

        boolean isStatic = Modifier.isStatic(cached.method.getModifiers());
        //Static methods have no instance
        Object receiver = isStatic || instance == null ? null : instance;

        return new MethodRef(cached.handle, cached.spreadHandle, receiver);
    }

    public static <T> Ref<T> getField(Class<?> owner, Object instance, FieldQuerySpec query) {

        Optional<Field> found = lookupField(owner, query);
        if (found.isEmpty()) throw new RuntimeException("No field matching the query on " + owner.getName() + " with the query " + query);

        return new FieldRef<>(found.get(), instance);
    }

    public static boolean hasMethod(Class<?> owner, Object instance, MethodQuerySpec query) {
        return lookupMethod(owner, query).isPresent();
    }

    public static boolean hasField(Class<?> owner, Object instance, FieldQuerySpec query) {
        return lookupField(owner, query).isPresent();
    }

    /** get() first, as computeIfAbsent locks the bin when the key is not its first entry. */
    private static Optional<CachedMethod> lookupMethod(Class<?> owner, MethodQuerySpec query) {
        ConcurrentHashMap<MethodQuerySpec, Optional<CachedMethod>> cache = METHOD_CACHE.get(owner);
        Optional<CachedMethod> cached = cache.get(query);
        if (cached != null) return cached;
        return cache.computeIfAbsent(query, key -> {
            Method method = findMethod(owner, key);
            if (method == null) return Optional.empty();
            return Optional.of(createCachedMethod(method));
        });
    }

    private static Optional<Field> lookupField(Class<?> owner, FieldQuerySpec query) {
        ConcurrentHashMap<FieldQuerySpec, Optional<Field>> cache = FIELD_CACHE.get(owner);
        Optional<Field> cached = cache.get(query);
        if (cached != null) return cached;
        return cache.computeIfAbsent(query, key -> Optional.ofNullable(findField(owner, key)));
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
            //Fixed arity, otherwise asType below wraps the varargs array of the caller in to another array.
            MethodHandle handle = LOOKUP.unreflect(method).asFixedArity();
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
