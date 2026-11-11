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
import patchlib.agent.context.HookContextImpl;
import patchlib.agent.log.PatchLibLogger;
import patchlib.agent.patch.advice.AdvicePatchRegistry;
import patchlib.agent.patch.redirect.RedirectHandleChain;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Creates one small class per patch site, which keeps the sites method handle chains in static final fields.
 * The patched code reads those fields to get its chain. The Just-in-Time compiler treats static final fields as constants,
 * which allows it to optimise through the chain, and reading a static field works in every class file version.
 * The fields are assigned when the patched code reads them for the first time.
 */
public class HandleHolders {

    public final static String BEFORE = "BEFORE";
    public final static String AFTER = "AFTER";
    public final static String REDIRECT = "REDIRECT";

    //The methods the generated classes call to fill their fields
    private final static Method BEFORE_PROVIDER = getProvider("provideBefore");
    private final static Method AFTER_PROVIDER = getProvider("provideAfter");
    private final static Method REDIRECT_PROVIDER = getProvider("provideRedirect");

    private final static ByteBuddy BYTE_BUDDY = new ByteBuddy().with(TypeValidation.DISABLED);
    private final static MethodHandle EMPTY_ADVICE_CHAIN = MethodHandles.empty(MethodType.methodType(void.class, HookContextImpl.class));

    //Site ids are reused when a class is transformed again, so the holders are as well.
    private final static Map<Integer, Class<?>> ADVICE_HOLDERS = new ConcurrentHashMap<>();
    private final static Map<Integer, Class<?>> REDIRECT_HOLDERS = new ConcurrentHashMap<>();

    /** Returns the BEFORE or AFTER field of the holder for a hook site. The site has to be registered already. */
    public static Field getAdviceField(int siteId, String fieldName) {
        Class<?> holder = ADVICE_HOLDERS.computeIfAbsent(siteId, id -> createHolder("AdviceHolder" + id,
                assign(BEFORE_PROVIDER, id, BEFORE).andThen(assign(AFTER_PROVIDER, id, AFTER)), BEFORE, AFTER));
        return getField(holder, fieldName);
    }

    /** Returns the REDIRECT field of the holder for a redirect site. The site has to be registered already. */
    public static Field getRedirectField(int siteId) {
        Class<?> holder = REDIRECT_HOLDERS.computeIfAbsent(siteId, id -> createHolder("RedirectHolder" + id,
                assign(REDIRECT_PROVIDER, id, REDIRECT), REDIRECT));
        return getField(holder, REDIRECT);
    }

    public static MethodHandle provideBefore(int siteId) {
        try {
            return AdvicePatchRegistry.getSite(siteId).beforeChain;
        } catch (Throwable ex) {
            PatchLibLogger.error("Failed to provide the before chain for site " + siteId + ", its patches will be skipped", ex);
            return EMPTY_ADVICE_CHAIN;
        }
    }

    public static MethodHandle provideAfter(int siteId) {
        try {
            return AdvicePatchRegistry.getSite(siteId).afterChain;
        } catch (Throwable ex) {
            PatchLibLogger.error("Failed to provide the after chain for site " + siteId + ", its patches will be skipped", ex);
            return EMPTY_ADVICE_CHAIN;
        }
    }

    /** Unlike the hooks there is nothing to fall back to, as the chain also contains the code that the redirect replaced. */
    public static MethodHandle provideRedirect(int siteId) throws Throwable {
        try {
            return RedirectHandleChain.createHandleChain(siteId);
        } catch (Throwable ex) {
            PatchLibLogger.error("Failed to provide the redirect chain for site " + siteId + ", the redirected code can not run", ex);
            throw ex;
        }
    }

    /** The static initializer of the holder, assigning one field with the result of provider(siteId) */
    private static MethodCall.FieldSetting assign(Method provider, int siteId, String fieldName) {
        return MethodCall.invoke(provider).with(siteId).setsField(ElementMatchers.named(fieldName));
    }

    private static Class<?> createHolder(String name, Implementation initializer, String... fieldNames) {
        DynamicType.Builder<Object> builder = BYTE_BUDDY.subclass(Object.class, ConstructorStrategy.Default.NO_CONSTRUCTORS)
                .name(HandleHolders.class.getPackageName() + "." + name);

        for (String fieldName : fieldNames) {
            builder = builder.defineField(fieldName, MethodHandle.class, Visibility.PUBLIC, Ownership.STATIC, FieldManifestation.FINAL);
        }

        //Defined through a lookup of this class, which places the holder next to it, in the same package and classloader.
        //Patched classes can already see the agents classes, so they can see the holder as well.
        return builder.invokable(ElementMatchers.isTypeInitializer()).intercept(initializer)
                .make()
                .load(HandleHolders.class.getClassLoader(), ClassLoadingStrategy.UsingLookup.of(MethodHandles.lookup()))
                .getLoaded();
    }

    private static Field getField(Class<?> holder, String fieldName) {
        try {
            return holder.getField(fieldName);
        } catch (NoSuchFieldException ex) {
            throw new RuntimeException("Could not find the field " + fieldName + " on " + holder.getName(), ex);
        }
    }

    private static Method getProvider(String methodName) {
        try {
            return HandleHolders.class.getMethod(methodName, int.class);
        } catch (NoSuchMethodException ex) {
            throw new RuntimeException("Could not find provider method", ex);
        }
    }

}
