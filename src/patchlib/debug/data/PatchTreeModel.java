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
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

/** Snapshot of the patch registry as a class -> method -> handler tree, plus the specs that scanned but
 * never installed anywhere. Built fresh each time so it reflects classes that loaded since the last build. */
public final class PatchTreeModel {

    /** One patch handler on a method. interceptTarget is the redirected member for redirects, null otherwise. */
    public record HandlerRow(String kind, int priority, String modName, String handler, String interceptTarget) { }

    public record MethodNode(String key, String label, List<HandlerRow> handlers) { }

    public record ClassNode(String className, List<MethodNode> methods, int handlerCount) { }

    /** A set of classes collapsed under a shared supertype, for the group-by-supertype view. */
    public record GroupNode(String label, List<ClassNode> classes, int handlerCount) { }

    /** One source mod's patches, as classes that carry only that mod's handlers, for the group-by-mod view. */
    public record ModNode(String modName, List<ClassNode> classes, int handlerCount) { }

    public record UninstalledRow(String modName, String kind, String target, String handler) { }

    /** Bucket for classes that share no supertype with any other; kept last so grouped rows stay one depth. */
    private static final String UNGROUPED = "(no shared supertype)";

    private final List<ClassNode> classes;
    private final List<GroupNode> groups;
    private final List<ModNode> mods;
    private final List<UninstalledRow> uninstalled;

    private PatchTreeModel(List<ClassNode> classes, List<GroupNode> groups, List<ModNode> mods, List<UninstalledRow> uninstalled) {
        this.classes = classes;
        this.groups = groups;
        this.mods = mods;
        this.uninstalled = uninstalled;
    }

    public List<ClassNode> classes() {
        return classes;
    }

    public List<GroupNode> groups() {
        return groups;
    }

    public List<ModNode> mods() {
        return mods;
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

        List<GroupNode> groups = buildGroups(classes, PatchRegistry.supertypeSnapshot());
        List<ModNode> mods = buildMods(classes);

        return new PatchTreeModel(classes, groups, mods, uninstalled);
    }

    /** Regroups the class tree by source mod: one node per mod, each holding copies of the classes it patches
     * that carry only that mod's handlers. Order follows the already-sorted class/method/handler lists. */
    private static List<ModNode> buildMods(List<ClassNode> classes) {
        Set<String> modNames = new TreeSet<>();
        for (ClassNode c : classes) {
            for (MethodNode m : c.methods()) {
                for (HandlerRow h : m.handlers()) modNames.add(h.modName());
            }
        }

        List<ModNode> mods = new ArrayList<>();
        for (String mod : modNames) {
            List<ClassNode> owned = new ArrayList<>();
            int total = 0;
            for (ClassNode c : classes) {
                ClassNode filtered = filterClassForMod(c, mod);
                if (filtered == null) continue;
                owned.add(filtered);
                total += filtered.handlerCount();
            }
            if (!owned.isEmpty()) mods.add(new ModNode(mod, owned, total));
        }
        return mods;
    }

    /** A copy of a class keeping only the methods and handlers belonging to the given mod, or null if none. */
    private static ClassNode filterClassForMod(ClassNode c, String mod) {
        List<MethodNode> methods = new ArrayList<>();
        int total = 0;
        for (MethodNode m : c.methods()) {
            List<HandlerRow> owned = new ArrayList<>();
            for (HandlerRow h : m.handlers()) {
                if (h.modName().equals(mod)) owned.add(h);
            }
            if (owned.isEmpty()) continue;
            methods.add(new MethodNode(m.key(), m.label(), owned));
            total += owned.size();
        }
        return methods.isEmpty() ? null : new ClassNode(c.className(), methods, total);
    }

    /** Collapses the flat class list into supertype groups: each class joins the supertype it shares with the
     * most other patched classes (needs at least two members), and the rest fall into one catch-all bucket. */
    private static List<GroupNode> buildGroups(List<ClassNode> classes, Map<String, Set<String>> supertypes) {
        Map<String, Integer> counts = new HashMap<>();
        for (ClassNode c : classes) {
            for (String supertype : supertypes.getOrDefault(c.className(), Set.of())) {
                counts.merge(supertype, 1, Integer::sum);
            }
        }

        Map<String, List<ClassNode>> byGroup = new HashMap<>();
        List<ClassNode> ungrouped = new ArrayList<>();
        for (ClassNode c : classes) {
            String best = null;
            int bestCount = 1;
            for (String supertype : supertypes.getOrDefault(c.className(), Set.of())) {
                int count = counts.getOrDefault(supertype, 0);
                if (count < 2) continue;
                if (count > bestCount || (count == bestCount && (best == null || supertype.compareTo(best) < 0))) {
                    best = supertype;
                    bestCount = count;
                }
            }
            if (best == null) ungrouped.add(c);
            else byGroup.computeIfAbsent(best, k -> new ArrayList<>()).add(c);
        }

        List<GroupNode> groups = new ArrayList<>();
        for (Map.Entry<String, List<ClassNode>> entry : byGroup.entrySet()) {
            List<ClassNode> members = entry.getValue();
            if (members.size() < 2) ungrouped.addAll(members); // a lone survivor is not worth its own group
            else groups.add(new GroupNode(entry.getKey(), members, handlerTotal(members)));
        }
        groups.sort(Comparator.comparing(GroupNode::label));

        if (!ungrouped.isEmpty()) {
            ungrouped.sort(Comparator.comparing(ClassNode::className));
            groups.add(new GroupNode(UNGROUPED, ungrouped, handlerTotal(ungrouped)));
        }
        return groups;
    }

    private static int handlerTotal(List<ClassNode> classes) {
        int total = 0;
        for (ClassNode c : classes) total += c.handlerCount();
        return total;
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
