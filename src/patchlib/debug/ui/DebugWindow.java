package patchlib.debug.ui;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.BaseCustomUIPanelPlugin;
import com.fs.starfarer.api.campaign.FactionSpecAPI;
import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.ui.Alignment;
import com.fs.starfarer.api.ui.CustomPanelAPI;
import com.fs.starfarer.api.ui.CutStyle;
import com.fs.starfarer.api.ui.Fonts;
import com.fs.starfarer.api.ui.LabelAPI;
import com.fs.starfarer.api.ui.PositionAPI;
import com.fs.starfarer.api.ui.TextFieldAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.ui.UIComponentAPI;
import com.fs.starfarer.api.ui.UIPanelAPI;
import com.fs.starfarer.api.util.Misc;
import patchlib.debug.DebugMenuManager;
import patchlib.debug.data.PatchTreeModel;
import patchlib.debug.data.PatchTreeModel.ClassNode;
import patchlib.debug.data.PatchTreeModel.HandlerRow;
import patchlib.debug.data.PatchTreeModel.MethodNode;
import patchlib.debug.data.PatchTreeModel.UninstalledRow;

import java.awt.Color;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** The debug window: a draggable panel with a vanilla header, toolbar and search, and the class -> method ->
 * handler tree. Backs its own panel (fill, border, drag, column header), builds all content, and owns the tree
 * state. The scrolling list lives in a container we recreate from scratch on every update, so nothing lingers. */
public class DebugWindow extends BaseCustomUIPanelPlugin implements PatchRow.Listener {

    public static final float WIDTH = 1120f;
    public static final float HEIGHT = 720f;

    private static final String FONT = Fonts.DEFAULT_SMALL; // one font for every row and the header

    private static final float MARGIN = 8f;
    private static final float TOP_PAD = 4f;
    private static final float GAP = 8f;
    private static final float BTN_H = 22f;
    private static final float HEADER_H = 26f;
    private static final float ROW_H = 26f;
    private static final float ROW_GAP = 3f;
    private static final float ROW_TEXT_Y = 5f;
    // the vanilla scroller's addCustom() shifts every row this far right; the header is offset to match
    private static final float ADDCUSTOM_INSET = 5f;
    private static final float SEARCH_W = 380f;
    private static final float HDR_GAP = 5f;
    private static final float CHAR_W = 7f;
    private static final float POLL = 0.2f;
    private static final int MAX_ROWS = 2000;

    // tree layout: a class name sits at NAME_X, indented one INDENT per level deeper; the caret sits just
    // left of the name, and deeper rows also inset their background bar.
    private static final float NAME_X = 22f;
    private static final float INDENT = 22f;
    private static final float CARET_GAP = 12f;
    private static final String[] COL_TITLES = {"Patch site  /  handler", "Kind", "Source mod", "Prio"};

    private static final Color FALLBACK_BASE = new Color(150, 170, 200);
    private static final Color FALLBACK_DARK = new Color(45, 55, 70);
    private static final Color FALLBACK_TEXT = new Color(220, 220, 220);

    private Color cBase, cDark, cText, cWarn, cClassBg, cMethodBg, cLeafBg;

    private CustomPanelAPI panel;
    private PositionAPI position;

    // drag: dx/dy are the panel's top-left offset in the parent (screen) space
    private float dx, dy, w, h, screenW, screenH, barH;
    private boolean dragging;
    private float grabMouseX, grabMouseY, grabDx, grabDy;

    private float contentX, contentW, rowW, headerRowTop, listTop, listH;
    private float kindX, modX, prioX;
    private float[] segX, segW;

    private TextFieldAPI searchField;
    private CustomPanelAPI listHost;
    private TooltipMakerAPI listElement;

    private PatchTreeModel model;
    private final Set<String> expandedClasses = new HashSet<>();
    private final Set<String> expandedMethods = new HashSet<>();

    private float pollTimer;
    private String lastSearch = "";
    private boolean needsRebuild; // a row click toggled state; rebuild on the next advance, not mid input-dispatch

    public CustomPanelAPI getPanel() {
        return panel;
    }

    public boolean contains(float x, float y) {
        if (position == null) return false;
        return x >= position.getX() && x <= position.getX() + position.getWidth()
                && y >= position.getY() && y <= position.getY() + position.getHeight();
    }

    public void create(UIPanelAPI parent, float savedX, float savedY, float screenW, float screenH) {
        buildPalette();
        this.screenW = screenW;
        this.screenH = screenH;
        this.w = Math.min(WIDTH, screenW - 40f);
        this.h = Math.min(HEIGHT, screenH - 40f);
        this.contentX = MARGIN;
        this.contentW = w - MARGIN * 2f;
        // usable row width: addCustom eats ADDCUSTOM_INSET on the left, the scroller reserves the same on the
        // right for its widget, so drop both.
        this.rowW = contentW - ADDCUSTOM_INSET * 2f;
        this.kindX = rowW - 380f;
        this.modX = rowW - 250f;
        this.prioX = rowW - 55f;
        computeColumns();

        panel = Global.getSettings().createCustom(w, h, this);

        buildTitle();
        float toolbarTop = TOP_PAD + barH + GAP;
        buildToolbar(toolbarTop);
        this.headerRowTop = toolbarTop + BTN_H + GAP;
        buildHeader();
        this.listTop = headerRowTop + HEADER_H + 3f;
        this.listH = h - listTop - MARGIN;

        this.dx = savedX >= 0 ? savedX : (screenW - w) / 2f;
        this.dy = savedY >= 0 ? savedY : (screenH - h) / 2f;
        this.dx = Math.max(0f, Math.min(dx, screenW - w));
        this.dy = Math.max(0f, Math.min(dy, screenH - h));
        parent.addComponent(panel).inTL(dx, dy);
        DebugMenuManager.saveWindowPos(dx, dy);

        model = PatchTreeModel.build();
        rebuildList(true);
    }

    private void buildPalette() {
        cBase = FALLBACK_BASE; cDark = FALLBACK_DARK;
        cText = FALLBACK_TEXT; cWarn = Color.YELLOW;
        try {
            FactionSpecAPI s = Global.getSettings().getFactionSpec("player");
            if (s != null) {
                if (s.getBaseUIColor() != null) cBase = s.getBaseUIColor();
                if (s.getDarkUIColor() != null) cDark = s.getDarkUIColor();
            }
        } catch (Exception e) { }
        try { if (Misc.getTextColor() != null) cText = Misc.getTextColor(); } catch (Exception e) { }
        try { if (Misc.getHighlightColor() != null) cWarn = Misc.getHighlightColor(); } catch (Exception e) { }
        cClassBg = Misc.scaleColorOnly(cDark, 0.8f);     // 1st level: dimmer than the header, above the 2nd
        cMethodBg = Misc.scaleColorOnly(cDark, 0.62f);   // 2nd level: darker
        cLeafBg = Misc.scaleColorOnly(cDark, 0.30f);     // 3rd level: darkest, near-black blue
    }

    private void computeColumns() {
        // 4 columns as [start, end] relative to the row's left edge; segments have a gap between them
        float[] start = {0f, kindX, modX, prioX};
        float[] end = {kindX - HDR_GAP, modX - HDR_GAP, prioX - HDR_GAP, rowW};
        segX = start;
        segW = new float[4];
        for (int i = 0; i < 4; i++) segW[i] = end[i] - start[i];
    }

    // --- chrome ---------------------------------------------------------------

    private void buildTitle() {
        TooltipMakerAPI header = panel.createUIElement(contentW, 30f, false);
        header.addSectionHeading("PatchLib Debug", Alignment.MID, 0f);
        barH = header.getHeightSoFar();
        if (barH < 16f) barH = 20f;
        panel.addUIElement(header).inTL(contentX, TOP_PAD);

        TooltipMakerAPI closeEl = panel.createUIElement(barH, barH, false);
        closeEl.addButton("X", "close", cBase, cDark, Alignment.MID, CutStyle.NONE, barH, barH, 0f);
        panel.addUIElement(closeEl).inTR(MARGIN, TOP_PAD);
    }

    @Override
    public void positionChanged(PositionAPI position) {
        this.position = position;
    }

    @Override
    public void renderBelow(float alphaMult) {
        if (position == null) return;
        float x = position.getX();
        float top = position.getY() + h;
        DebugRender.fillRect(x, position.getY(), w, h, Color.black, 0.9f * alphaMult);
        DebugRender.rectBorder(x, position.getY(), w, h, cBase, 0.4f * alphaMult);

        // column header split into per-column segments with gaps between them (aligned to the row inset)
        if (segX == null) return;
        float segY = top - headerRowTop - HEADER_H;
        float base = x + contentX + ADDCUSTOM_INSET;
        for (int i = 0; i < segX.length; i++) {
            DebugRender.fillRect(base + segX[i], segY, segW[i], HEADER_H, cDark, 0.92f * alphaMult);
        }
    }

    private boolean inTitleBar(float x, float y) {
        if (position == null) return false;
        float top = position.getY() + h;
        float closeLeft = position.getX() + w - MARGIN - barH;
        return x >= position.getX() && x < closeLeft
                && y >= top - (TOP_PAD + barH) && y <= top;
    }

    @Override
    public void processInput(List<InputEventAPI> events) {
        if (position == null) return;
        for (InputEventAPI event : events) {
            if (event.isConsumed()) continue;
            if (event.isLMBDownEvent() && inTitleBar(event.getX(), event.getY())) {
                dragging = true;
                grabMouseX = event.getX();
                grabMouseY = event.getY();
                grabDx = dx;
                grabDy = dy;
                event.consume();
            } else if (dragging && event.isMouseMoveEvent()) {
                dx = Math.max(0f, Math.min(grabDx + (event.getX() - grabMouseX), screenW - w));
                dy = Math.max(0f, Math.min(grabDy - (event.getY() - grabMouseY), screenH - h));
                position.inTL(dx, dy);
                DebugMenuManager.saveWindowPos(dx, dy);
                event.consume();
            } else if (dragging && event.isLMBUpEvent()) {
                dragging = false;
                event.consume();
            }
        }
    }

    // --- toolbar & header -----------------------------------------------------

    private void buildToolbar(float top) {
        float x = contentX;
        x += addActionButton("Refresh", "refresh", x, 76f, top) + 6f;
        x += addActionButton("Expand all", "expandall", x, 86f, top) + 6f;
        x += addActionButton("Collapse all", "collapseall", x, 92f, top) + GAP;

        TooltipMakerAPI searchEl = panel.createUIElement(SEARCH_W, BTN_H, false);
        searchField = searchEl.addTextField(SEARCH_W, BTN_H, FONT, 0f);
        searchField.setHandleCtrlV(true);
        searchField.setUndoOnEscape(false);
        panel.addUIElement(searchEl).inTL(x, top);
    }

    private float addActionButton(String text, String data, float x, float bw, float top) {
        TooltipMakerAPI el = panel.createUIElement(bw, BTN_H, false);
        el.addButton(text, data, cBase, cDark, Alignment.MID, CutStyle.NONE, bw, BTN_H, 0f);
        panel.addUIElement(el).inTL(x, top);
        return bw;
    }

    private void buildHeader() {
        float y = headerRowTop + ROW_TEXT_Y;
        float base = contentX + ADDCUSTOM_INSET;
        placeLabel(panel, COL_TITLES[0], cBase, base + NAME_X, segW[0] - NAME_X, false, y);
        for (int i = 1; i < COL_TITLES.length; i++) {
            placeLabel(panel, COL_TITLES[i], cBase, base + segX[i], segW[i], true, y);
        }
    }

    /** Adds a label to a panel, left-aligned at x or centered within [x, x + maxWidth]. */
    private void placeLabel(UIPanelAPI target, String text, Color color, float x, float maxWidth, boolean center, float y) {
        String txt = fit(text, maxWidth);
        LabelAPI label = Global.getSettings().createLabel(txt, FONT);
        label.setColor(color);
        float lx = x;
        if (center) lx = x + Math.max(0f, (maxWidth - label.computeTextWidth(txt)) / 2f);
        target.addComponent((UIComponentAPI) label).inTL(lx, y);
    }

    // --- the tree list --------------------------------------------------------

    /** Rebuilds the list. Pass resetScroll when the content changes wholesale (search, collapse all), so a
     * kept offset can't strand the view past the end of now-shorter content. */
    private void rebuildList(boolean resetScroll) {
        float prevScroll = 0f;
        if (!resetScroll && listElement != null && listElement.getExternalScroller() != null) {
            prevScroll = listElement.getExternalScroller().getYOffset();
        }
        if (listHost != null) panel.removeComponent(listHost);

        listHost = panel.createCustomPanel(contentW, listH, new BaseCustomUIPanelPlugin() { });
        listElement = listHost.createUIElement(contentW, listH, true);

        String q = searchField == null ? "" : searchField.getText().toLowerCase();
        lastSearch = q;
        boolean searching = !q.isEmpty();

        int emitted = 0;
        for (ClassNode c : model.classes()) {
            if (searching && !classMatches(c, q)) continue;
            if (emitted >= MAX_ROWS) break;
            boolean classExpanded = searching || expandedClasses.contains(c.className());
            classRow(c, classExpanded);
            emitted++;
            if (!classExpanded) continue;

            boolean classNameHit = contains(c.className(), q);
            for (MethodNode m : c.methods()) {
                if (searching && !classNameHit && !methodMatches(m, q)) continue;
                if (emitted >= MAX_ROWS) break;
                boolean methodExpanded = searching || expandedMethods.contains(m.key());
                methodRow(m, methodExpanded);
                emitted++;
                if (!methodExpanded) continue;
                for (HandlerRow hh : m.handlers()) {
                    if (emitted >= MAX_ROWS) break;
                    handlerRow(hh);
                    emitted++;
                }
            }
        }

        if (!model.uninstalled().isEmpty()) {
            sectionRow("Discovered but not installed (either nothing matched, or a matching class has not been loaded yet)");
            for (UninstalledRow u : model.uninstalled()) {
                if (searching && !(contains(u.modName(), q) || contains(u.kind(), q)
                        || contains(u.target(), q) || contains(u.handler(), q))) continue;
                if (emitted >= MAX_ROWS) break;
                uninstalledRow(u);
                emitted++;
            }
        }
        if (emitted >= MAX_ROWS) sectionRow("Row limit reached (" + MAX_ROWS + "); narrow the search to see more");

        listHost.addUIElement(listElement).inTL(0f, 0f);
        panel.addComponent(listHost).inTL(contentX, listTop);
        if (listElement.getExternalScroller() != null) listElement.getExternalScroller().setYOffset(prevScroll);
    }

    /** An expandable class/method row: a bar with a caret and a single label; the whole row acts as a button. */
    private void expandableRow(String label, String clickId, Color bg, int level, boolean expanded) {
        float inset = level * INDENT;
        float nameX = NAME_X + inset;
        CustomPanelAPI rowPanel = listHost.createCustomPanel(rowW, ROW_H,
                new PatchRow(bg, 1f, inset, cBase, expanded, nameX - CARET_GAP, clickId, this));
        placeLabel(rowPanel, label, cBase, nameX, rowW - nameX - HDR_GAP, false, ROW_TEXT_Y);
        listElement.addCustom(rowPanel, ROW_GAP);
    }

    /** A leaf handler/section row: an inset background bar plus column labels; not clickable, no caret. */
    private void leafRow(Color bg, float bgAlpha, int level, List<Cell> cells) {
        CustomPanelAPI rowPanel = listHost.createCustomPanel(rowW, ROW_H,
                new PatchRow(bg, bgAlpha, level * INDENT, cBase, null, 0f, null, null));
        for (Cell cell : cells) {
            if (cell.text == null || cell.text.isEmpty()) continue;
            placeLabel(rowPanel, cell.text, cell.color, cell.x, cell.maxWidth, cell.center, ROW_TEXT_Y);
        }
        listElement.addCustom(rowPanel, ROW_GAP);
    }

    private void classRow(ClassNode c, boolean expanded) {
        String label = c.className() + "   (" + c.handlerCount() + ")";
        expandableRow(label, "cls:" + c.className(), cClassBg, 0, expanded);
    }

    private void methodRow(MethodNode m, boolean expanded) {
        expandableRow(m.label(), "mtd:" + m.key(), cMethodBg, 1, expanded);
    }

    /** A level-2 data row: name in column 0, then kind / mod / prio; an empty prio cell is skipped. */
    private void dataRow(String name, String kind, String mod, String prio) {
        float nameX = NAME_X + 2 * INDENT;
        leafRow(cLeafBg, 0.95f, 2, List.of(
                new Cell(name, cText, nameX, segW[0] - nameX, false),
                new Cell(kind, cText, segX[1], segW[1], true),
                new Cell(mod, cText, segX[2], segW[2], true),
                new Cell(prio, cText, segX[3], segW[3], true)));
    }

    private void handlerRow(HandlerRow hh) {
        String name = hh.handler();
        if (hh.interceptTarget() != null) name += "  ->  " + hh.interceptTarget();
        dataRow(name, hh.kind(), hh.modName(), String.valueOf(hh.priority()));
    }

    private void sectionRow(String text) {
        leafRow(cWarn, 0.15f, 0, List.of(new Cell(text, cWarn, NAME_X, rowW - NAME_X, false)));
    }

    private void uninstalledRow(UninstalledRow u) {
        dataRow(u.handler() + "  ->  " + u.target(), u.kind(), u.modName(), "");
    }

    private static String fit(String text, float maxWidth) {
        if (maxWidth <= 0) return text;
        int maxChars = Math.max(1, (int) (maxWidth / CHAR_W));
        if (text.length() <= maxChars) return text;
        return text.substring(0, Math.max(1, maxChars - 1)) + "…";
    }

    // --- filtering & events ---------------------------------------------------

    private static boolean contains(String value, String query) {
        return value != null && value.toLowerCase().contains(query);
    }

    private boolean handlerMatches(HandlerRow hh, String q) {
        return contains(hh.kind(), q) || contains(hh.modName(), q) || contains(hh.handler(), q)
                || contains(hh.interceptTarget(), q);
    }

    private boolean methodMatches(MethodNode m, String q) {
        if (contains(m.label(), q)) return true;
        for (HandlerRow hh : m.handlers()) if (handlerMatches(hh, q)) return true;
        return false;
    }

    private boolean classMatches(ClassNode c, String q) {
        if (contains(c.className(), q)) return true;
        for (MethodNode m : c.methods()) if (methodMatches(m, q)) return true;
        return false;
    }

    @Override
    public void onRowClicked(String id) {
        if (id.startsWith("cls:")) toggle(expandedClasses, id.substring(4));
        else if (id.startsWith("mtd:")) toggle(expandedMethods, id.substring(4));
        // fired from a row's processInput; defer the rebuild so we don't drop the list mid-dispatch
        needsRebuild = true;
    }

    private void toggle(Set<String> set, String value) {
        if (!set.remove(value)) set.add(value);
    }

    private void expandAll() {
        for (ClassNode c : model.classes()) {
            expandedClasses.add(c.className());
            for (MethodNode m : c.methods()) expandedMethods.add(m.key());
        }
    }

    @Override
    public void advance(float amount) {
        if (needsRebuild) {
            needsRebuild = false;
            rebuildList(false); // expand/collapse: keep the user where they were
        }
        pollTimer += amount;
        if (pollTimer < POLL) return;
        pollTimer = 0f;
        String search = searchField == null ? "" : searchField.getText().toLowerCase();
        if (!search.equals(lastSearch)) rebuildList(true); // search changed: back to the top
    }

    @Override
    public void buttonPressed(Object data) {
        if (!(data instanceof String s)) return;
        switch (s) {
            case "close" -> DebugMenuManager.close();
            case "refresh" -> { model = PatchTreeModel.build(); rebuildList(false); }
            case "expandall" -> { expandAll(); rebuildList(false); }
            case "collapseall" -> { expandedClasses.clear(); expandedMethods.clear(); rebuildList(true); }
            default -> { }
        }
    }

    private record Cell(String text, Color color, float x, float maxWidth, boolean center) { }
}
