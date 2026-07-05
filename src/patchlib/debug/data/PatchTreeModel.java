package patchlib.debug.data;

import patchlib.agent.PatchHandler;
import patchlib.agent.PatchRegistry;
import patchlib.agent.PatchSite;
import patchlib.agent.RedirectSite;
import patchlib.agent.spec.PatchSpec;
import patchlib.agent.spec.PatchType;
import patchlib.agent.spec.RedirectKind;
import patchlib.agent.spec.TargetClassSpec;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/** Snapshot of the patch registry as a class -> method -> handler tree, plus the specs that scanned but
 * never installed anywhere. Built fresh each time so it reflects classes that loaded since the last build. */
public final class PatchTreeModel {

    /** One patch handler on a method. interceptTarget is the redirected member for redirects, null otherwise. */
    public record HandlerRow(String kind, int priority, String modName, String handler, String interceptTarget) { }

    public record MethodNode(String key, String label, List<HandlerRow> handlers) { }

    public record ClassNode(String className, List<MethodNode> methods, int handlerCount) { }

    public record UninstalledRow(String modName, String kind, String target, String handler) { }

    private final List<ClassNode> classes;
    private final List<UninstalledRow> uninstalled;

    private PatchTreeModel(List<ClassNode> classes, List<UninstalledRow> uninstalled) {
        this.classes = classes;
        this.uninstalled = uninstalled;
    }

    public List<ClassNode> classes() {
        return classes;
    }

    public List<UninstalledRow> uninstalled() {
        return uninstalled;
    }

    private static final class MethodAccum {
        final String label;
        final List<HandlerRow> rows = new ArrayList<>();
        MethodAccum(String label) { this.label = label; }
    }

    public static PatchTreeModel build() {
        Map<String, Map<String, MethodAccum>> byClass = new TreeMap<>();
        IdentityHashMap<PatchSpec, Boolean> installed = new IdentityHashMap<>();

        for (Map.Entry<String, PatchSite> entry : PatchRegistry.siteSnapshot().entrySet()) {
            String key = entry.getKey();
            PatchSite site = entry.getValue();
            addAdvice(byClass, installed, key, "Before", site.beforePatches());
            addAdvice(byClass, installed, key, "After", site.afterPatches());
            addAdvice(byClass, installed, key, "Except", site.exceptPatches());
        }

        for (Map.Entry<String, RedirectSite> entry : PatchRegistry.redirectSnapshot().entrySet()) {
            String key = entry.getKey();
            RedirectSite site = entry.getValue();
            String separator = "->" + site.kind() + ":";
            int idx = key.indexOf(separator);
            String hostKey = idx >= 0 ? key.substring(0, idx) : key;
            String originalKey = idx >= 0 ? key.substring(idx + separator.length()) : "";
            String kind = redirectLabel(site.kind());
            String target = DescriptorFormat.memberWithClass(originalKey);
            for (PatchHandler handler : site.layers()) {
                addRow(byClass, installed, hostKey, kind, handler, target);
            }
        }

        List<ClassNode> classes = new ArrayList<>();
        for (Map.Entry<String, Map<String, MethodAccum>> classEntry : byClass.entrySet()) {
            List<MethodNode> methods = new ArrayList<>();
            int handlerCount = 0;
            List<MethodAccum> accums = new ArrayList<>(classEntry.getValue().values());
            accums.sort(Comparator.comparing(a -> a.label));
            for (MethodAccum accum : accums) {
                accum.rows.sort(Comparator.comparingInt(HandlerRow::priority).thenComparing(HandlerRow::kind));
                methods.add(new MethodNode(classEntry.getKey() + "#" + accum.label, accum.label, accum.rows));
                handlerCount += accum.rows.size();
            }
            classes.add(new ClassNode(classEntry.getKey(), methods, handlerCount));
        }

        List<UninstalledRow> uninstalled = new ArrayList<>();
        for (PatchSpec spec : PatchRegistry.scannedSpecs()) {
            if (installed.containsKey(spec)) continue;
            uninstalled.add(new UninstalledRow(modName(spec), kindOf(spec), targetSummary(spec), handlerLabel(spec)));
        }
        uninstalled.sort(Comparator.comparing(UninstalledRow::target).thenComparing(UninstalledRow::handler));

        return new PatchTreeModel(classes, uninstalled);
    }

    private static void addAdvice(Map<String, Map<String, MethodAccum>> byClass, IdentityHashMap<PatchSpec, Boolean> installed,
                                  String methodKey, String kind, PatchHandler[] handlers) {
        if (handlers == null) return;
        for (PatchHandler handler : handlers) {
            addRow(byClass, installed, methodKey, kind, handler, null);
        }
    }

    private static void addRow(Map<String, Map<String, MethodAccum>> byClass, IdentityHashMap<PatchSpec, Boolean> installed,
                               String methodKey, String kind, PatchHandler handler, String interceptTarget) {
        PatchSpec spec = handler.spec();
        installed.put(spec, Boolean.TRUE);
        String className = DescriptorFormat.className(methodKey);
        String label = DescriptorFormat.memberLabel(methodKey);
        MethodAccum accum = byClass.computeIfAbsent(className, k -> new TreeMap<>())
                .computeIfAbsent(methodKey, k -> new MethodAccum(label));
        accum.rows.add(new HandlerRow(kind, spec.priority(), modName(spec), handlerLabel(spec), interceptTarget));
    }

    private static String redirectLabel(RedirectKind kind) {
        return switch (kind) {
            case METHOD_CALL -> "RedirectCall";
            case CONSTRUCTOR -> "RedirectNew";
            case FIELD_READ -> "RedirectFieldRead";
            case FIELD_WRITE -> "RedirectFieldWrite";
        };
    }

    private static String kindOf(PatchSpec spec) {
        if (spec.patchType() == PatchType.REDIRECT && spec.redirectSite() != null) {
            return redirectLabel(spec.redirectSite().kind());
        }
        return switch (spec.patchType()) {
            case BEFORE -> "Before";
            case AFTER -> "After";
            case EXCEPT -> "Except";
            case REDIRECT -> "Redirect";
        };
    }

    private static String targetSummary(PatchSpec spec) {
        TargetClassSpec tc = spec.targetClass();
        String cls;
        if (tc == null) {
            cls = "(any class)";
        } else if (!tc.targetClass().isEmpty()) {
            cls = tc.targetClass();
        } else if (!tc.targetSubtype().isEmpty()) {
            cls = "subtype " + tc.targetSubtype();
        } else if (!tc.targetPackage().isEmpty()) {
            cls = "pkg " + tc.targetPackage();
        } else {
            cls = "(shape match)";
        }
        String method = spec.targetMethod() != null && !spec.targetMethod().methodName().isEmpty()
                ? spec.targetMethod().methodName() : "(any method)";
        return cls + " :: " + method;
    }

    private static String modName(PatchSpec spec) {
        try {
            return spec.sourceMod() != null ? spec.sourceMod().getName() : "?";
        } catch (Exception e) {
            return "?";
        }
    }

    private static String handlerLabel(PatchSpec spec) {
        return DescriptorFormat.simpleName(spec.handlerClass()) + "#" + spec.handlerMethod();
    }
}
