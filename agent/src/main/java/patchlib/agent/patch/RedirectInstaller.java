package patchlib.agent.patch;

import net.bytebuddy.asm.AsmVisitorWrapper;
import net.bytebuddy.asm.MemberSubstitution;
import net.bytebuddy.description.field.FieldDescription;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.matcher.ElementMatcher;
import net.bytebuddy.matcher.ElementMatchers;
import patchlib.agent.PatchHandler;
import patchlib.agent.PatchLibLogger;
import patchlib.agent.matchers.FieldTargetMatcher;
import patchlib.agent.matchers.MethodTargetMatcher;
import patchlib.agent.spec.RedirectKind;

import java.lang.invoke.MethodHandle;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/** Installs the redirects of one host method. All redirects of one kind share a single MemberSubstitution whose
 * matcher is the union of their call matchers. Grouping into layered sites happens during instrumentation, where
 * the resolved call is known, so divergent queries on the same call collapse together (see RedirectSubstitutionFactory). */
final class RedirectInstaller {

    private RedirectInstaller() {}

    static DynamicType.Builder<?> install(DynamicType.Builder<?> builder, TypeDescription type,
                                          MethodDescription.InDefinedShape method, List<InstallData> redirects) {
        //Redirects only exist as constant dispatch. Hosts below class file 55 previously failed on the
        //method handle constant anyway (janino emits 45), so there is no legacy path to fall back to.
        if (!PatchInstaller.supportsConstants(type)) {
            PatchLibLogger.warn("Redirects need class file version 55 or newer, skipping " + redirects.size()
                    + " redirect(s) at " + type.getActualName() + " in method " + method.getActualName());
            return builder;
        }

        String hostKey = PatchInstaller.memberKey(method);

        Map<RedirectKind, List<InstallData>> byKind = new EnumMap<>(RedirectKind.class);
        for (InstallData data : redirects) {
            byKind.computeIfAbsent(data.spec().redirectSite().kind(), k -> new ArrayList<>()).add(data);
        }

        for (Map.Entry<RedirectKind, List<InstallData>> entry : byKind.entrySet()) {
            builder = builder.visit(redirectVisitor(entry.getKey(), entry.getValue(), hostKey, type, method));
            PatchLibLogger.info("Installed redirects (" + entry.getKey() + ") at " + type.getActualName() + " in method " + method.getActualName());
        }
        return builder;
    }

    /** Builds one MemberSubstitution for a kind of redirect in a host method. The selector matches any call any of the
     * redirects wants; the factory then groups them per resolved call and delegates to the matching bridge. */
    private static AsmVisitorWrapper redirectVisitor(RedirectKind kind, List<InstallData> kindData, String hostKey,
                                                     TypeDescription hostType, MethodDescription method) {
        List<RedirectSubstitutionFactory.Layer> layers = new ArrayList<>();
        MemberSubstitution.WithoutSpecification<MemberSubstitution.Target.ForMember> target;

        if (kind == RedirectKind.METHOD_CALL) {
            ElementMatcher.Junction<MethodDescription> selector = ElementMatchers.none();
            for (InstallData data : kindData) {
                ElementMatcher.Junction<MethodDescription> matcher = MethodTargetMatcher.create(data.spec().redirectSite());
                selector = selector.or(matcher);
                layers.add(layer(MethodDescription.class, matcher, data));
            }
            target = MemberSubstitution.relaxed().method(selector);
        } else if (kind == RedirectKind.CONSTRUCTOR) {
            ElementMatcher.Junction<MethodDescription> selector = ElementMatchers.none();
            for (InstallData data : kindData) {
                ElementMatcher.Junction<MethodDescription> matcher = MethodTargetMatcher.createConstructor(data.spec().redirectSite());
                selector = selector.or(matcher);
                layers.add(layer(MethodDescription.class, matcher, data));
            }
            //this()/super() delegation uses the same instruction as a new expression and must not be substituted, so
            //in a constructor host every construction of the host class or its direct superclass is excluded.
            if (method.isConstructor()) {
                selector = selector.and(ElementMatchers.not(ElementMatchers.isDeclaredBy(hostType)));
                TypeDescription.Generic superClass = hostType.getSuperClass();
                if (superClass != null)
                    selector = selector.and(ElementMatchers.not(ElementMatchers.isDeclaredBy(superClass.asErasure())));
            }
            target = MemberSubstitution.relaxed().constructor(selector);
        } else {
            ElementMatcher.Junction<FieldDescription> selector = ElementMatchers.none();
            for (InstallData data : kindData) {
                ElementMatcher.Junction<FieldDescription> matcher = FieldTargetMatcher.create(data.spec().redirectSite());
                selector = selector.or(matcher);
                layers.add(layer(FieldDescription.class, matcher, data));
            }
            target = kind == RedirectKind.FIELD_READ
                    ? MemberSubstitution.relaxed().field(selector).onRead()
                    : MemberSubstitution.relaxed().field(selector).onWrite();
        }

        RedirectSubstitutionFactory factory = new RedirectSubstitutionFactory(kind, hostKey, hostType, bridgeFor(kind), staticCallBridgeFor(kind), layers);
        return target.replaceWith(factory).on(ElementMatchers.is(method));
    }

    /** One redirect layer: how to recognise its member against the resolved call, and the handler to run. */
    private static <T> RedirectSubstitutionFactory.Layer layer(Class<T> memberType, ElementMatcher.Junction<T> matcher, InstallData data) {
        return new RedirectSubstitutionFactory.Layer(
                member -> memberType.isInstance(member) && matcher.matches(memberType.cast(member)),
                new PatchHandler(data.spec(), data.handlerMethod(), data.blame()));
    }

    private static Method bridgeFor(RedirectKind kind) {
        try {
            return switch (kind) {
                case METHOD_CALL -> RedirectBridges.class.getMethod("methodCall",
                        MethodHandle.class, Object.class, Object[].class, Object.class, Object[].class);
                case CONSTRUCTOR -> RedirectBridges.class.getMethod("constructorCall",
                        MethodHandle.class, Object[].class, Object.class, Object[].class);
                case FIELD_READ -> RedirectBridges.class.getMethod("fieldRead",
                        MethodHandle.class, Object.class, Object.class, Object[].class);
                case FIELD_WRITE -> RedirectBridges.class.getMethod("fieldWrite",
                        MethodHandle.class, Object.class, Object[].class, Object.class, Object[].class);
            };
        } catch (NoSuchMethodException e) {
            throw new RuntimeException("Could not resolve redirect bridge for " + kind, e);
        }
    }

    /** The bridge variant used when a redirected call resolves to a static method, see RedirectBridges.methodCallStatic. */
    private static Method staticCallBridgeFor(RedirectKind kind) {
        if (kind != RedirectKind.METHOD_CALL) return null;
        try {
            return RedirectBridges.class.getMethod("methodCallStatic",
                    MethodHandle.class, Object.class, Object[].class, Object.class, Object[].class);
        } catch (NoSuchMethodException e) {
            throw new RuntimeException("Could not resolve the static call redirect bridge", e);
        }
    }
}
