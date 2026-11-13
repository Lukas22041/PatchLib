package patchlib.agent.patch.holder;

import net.bytebuddy.ByteBuddy;
import net.bytebuddy.description.modifier.FieldManifestation;
import net.bytebuddy.description.modifier.Ownership;
import net.bytebuddy.description.modifier.Visibility;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.dynamic.loading.ClassLoadingStrategy;
import net.bytebuddy.dynamic.scaffold.TypeValidation;
import net.bytebuddy.dynamic.scaffold.subclass.ConstructorStrategy;
import net.bytebuddy.implementation.Implementation;
import net.bytebuddy.implementation.MethodCall;
import net.bytebuddy.matcher.ElementMatchers;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * Generates holder classes while the game runs. A holder belongs to one patch site and only consists of static final fields,
 * which patched code reads to get the method handle chain of its site. A generated holder is the same as this written out class:
 *
 * <pre>
 * public class AdviceHolder12 {
 *     public static final MethodHandle BEFORE = AdviceHolders.provideBefore(12);
 *     public static final MethodHandle AFTER = AdviceHolders.provideAfter(12);
 * }
 * </pre>
 *
 * The Just-in-Time compiler treats static final fields as constants, which allows it to optimise through the chain,
 * and reading a static field works in every class file version. Like in any java class, the fields are assigned once,
 * when something reads from the class for the first time.
 */
public class HolderClassGenerator {

    /**
     * One field of a holder.
     * @param provider the method that returns the value of the field. Has to be public, static and take the site id as its only parameter.
     */
    public record HolderField(String name, Method provider) {

        public static HolderField of(String name, Class<?> providerClass, String providerMethodName) {
            try {
                return new HolderField(name, providerClass.getMethod(providerMethodName, int.class));
            } catch (NoSuchMethodException ex) {
                throw new RuntimeException("Could not find the provider method " + providerMethodName + " in " + providerClass.getName(), ex);
            }
        }
    }

    private static final ByteBuddy BYTE_BUDDY = new ByteBuddy().with(TypeValidation.DISABLED);
    private static final String HOLDER_PACKAGE = HolderClassGenerator.class.getPackageName();

    public static Class<?> generate(String className, int siteId, HolderField... fields) {
        DynamicType.Builder<Object> holder = BYTE_BUDDY
                .subclass(Object.class, ConstructorStrategy.Default.NO_CONSTRUCTORS)
                .name(HOLDER_PACKAGE + "." + className);

        holder = declareFields(holder, fields);
        holder = assignFieldsInStaticInitializer(holder, siteId, fields);

        return load(holder);
    }

    /** Looks up a field on a generated holder, in the form the installers need for binding it in to patched code. */
    public static Field getField(Class<?> holder, HolderField field) {
        try {
            return holder.getField(field.name());
        } catch (NoSuchFieldException ex) {
            throw new RuntimeException("Could not find the field " + field.name() + " on " + holder.getName(), ex);
        }
    }

    /** Adds "public static final MethodHandle NAME;" for each field. */
    private static DynamicType.Builder<Object> declareFields(DynamicType.Builder<Object> holder, HolderField... fields) {
        for (HolderField field : fields) {
            holder = holder.defineField(field.name(), MethodHandle.class, Visibility.PUBLIC, Ownership.STATIC, FieldManifestation.FINAL);
        }
        return holder;
    }

    /** Adds "static { NAME = provider(siteId); }", with one assignment per field. */
    private static DynamicType.Builder<Object> assignFieldsInStaticInitializer(DynamicType.Builder<Object> holder, int siteId, HolderField... fields) {
        Implementation.Composable assignments = assignment(fields[0], siteId);
        for (int i = 1; i < fields.length; i++) {
            assignments = assignments.andThen(assignment(fields[i], siteId));
        }
        return holder.invokable(ElementMatchers.isTypeInitializer()).intercept(assignments);
    }

    /** A single "NAME = provider(siteId);" */
    private static Implementation.Composable assignment(HolderField field, int siteId) {
        return MethodCall.invoke(field.provider())
                .with(siteId)
                .setsField(ElementMatchers.named(field.name()));
    }

    /**
     * Turns the description of the holder in to a loaded class. It is placed in the package and classloader of this class.
     * Patched classes can already see the agents classes, so they can see the holder as well.
     */
    private static Class<?> load(DynamicType.Builder<Object> holder) {
        ClassLoader agentLoader = HolderClassGenerator.class.getClassLoader();
        MethodHandles.Lookup agentLookup = MethodHandles.lookup();

        return holder.make()
                .load(agentLoader, ClassLoadingStrategy.UsingLookup.of(agentLookup))
                .getLoaded();
    }

}
