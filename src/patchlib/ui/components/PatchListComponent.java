package patchlib.ui.components;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.ModSpecAPI;
import com.fs.starfarer.api.campaign.BaseCustomUIPanelPlugin;
import com.fs.starfarer.api.graphics.SpriteAPI;
import com.fs.starfarer.api.ui.Alignment;
import com.fs.starfarer.api.ui.BaseTooltipCreator;
import com.fs.starfarer.api.ui.CustomPanelAPI;
import com.fs.starfarer.api.ui.CutStyle;
import com.fs.starfarer.api.ui.Fonts;
import com.fs.starfarer.api.ui.LabelAPI;
import com.fs.starfarer.api.ui.PositionAPI;
import com.fs.starfarer.api.ui.TextFieldAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.ui.UIPanelAPI;
import com.fs.starfarer.api.util.Misc;
import patchlib.agent.patch.PatchSiteRegistry;
import patchlib.agent.patch.PatchStateRegistry;
import patchlib.agent.spec.PatchHandlerSpec;
import patchlib.ui.elements.BaseElement;
import patchlib.ui.elements.ButtonElement;
import patchlib.ui.elements.CheckboxElement;
import patchlib.ui.elements.ListRowElement;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Scrollable and searchable list of all patches, which expand in to their handlers and those in to their installed methods. */
public class PatchListComponent extends BaseCustomUIPanelPlugin {

    private static final String PATCHLIB_ID = "patchlib";
    private static final String LUNA_CONFIG = "data/config/LunaSettingsConfig.json";

    public static final float HEADER_RIGHT_PAD = 10f;
    private static final float BUTTON_SIZE = 24f;
    private static final float SEARCH_TEXT_PAD = 6f;
    private static final float COLUMN_HEADING_Y = 30f;
    private static final float COLUMN_HEADING_HEIGHT = 18f;
    private static final float COLUMN_HEADING_OVERHANG = 10f;
    private static final float COLUMN_PAD = 3f;
    private static final float MOD_COLUMN_WIDTH = 40f;
    private static final float COUNT_COLUMN_WIDTH = BUTTON_SIZE * 2f + COLUMN_PAD;
    private static final float LIST_Y = 54f;
    private static final float LIST_X_OFFSET = 5f;
    private static final float ROW_HEIGHT = 22f;
    private static final float ICON_SIZE = 20f;
    private static final float INDENT = 7f;
    private static final float PATCH_PAD = 5f;
    private static final float TOOLTIP_WIDTH = 450f;
    private static final float TOOLTIP_LINE_PAD = 5f;
    private static final float HANDLER_BRIGHTNESS = 0.8f;
    private static final float SITE_BRIGHTNESS = 0.65f;

    private static final Map<String, String> modIcons = new HashMap<>();
    private static final Set<String> expandedKeys = new HashSet<>();
    private static float lastScroller = 0f;
    private static String lastSearch = "";

    private final CustomPanelAPI panel;
    private final TextFieldAPI searchField;
    private final LabelAPI searchLabel;
    private final float width;
    private final float height;
    private final float rowWidth;

    private CustomPanelAPI subpanel;
    private TooltipMakerAPI subelement;
    private String query = "";
    private boolean needsRecreate = false;

    public PatchListComponent(UIPanelAPI parent, float width, float height) {
        this.width = width;
        this.height = height;
        this.rowWidth = width - HEADER_RIGHT_PAD - LIST_X_OFFSET;

        panel = Global.getSettings().createCustom(width, height, this);
        parent.addComponent(panel);

        ButtonElement collapseAll = new ButtonElement(panel, "-", BUTTON_SIZE, BUTTON_SIZE, CutStyle.NONE);
        collapseAll.getPosition().inTR(HEADER_RIGHT_PAD, 0f);
        collapseAll.onPressed(() -> {
            expandedKeys.clear();
            needsRecreate = true;
        });

        ButtonElement expandAll = new ButtonElement(panel, "+", BUTTON_SIZE, BUTTON_SIZE, CutStyle.NONE);
        expandAll.getPosition().inTR(HEADER_RIGHT_PAD + BUTTON_SIZE + COLUMN_PAD, 0f);
        expandAll.onPressed(() -> {
            for (PatchHandlerSpec handler : PatchSiteRegistry.getHandlers()) {
                expandedKeys.add(handler.handlerClassName());
                expandedKeys.add(getKey(handler));
            }
            needsRecreate = true;
        });

        float columnsWidth = width - HEADER_RIGHT_PAD;
        float searchWidth = columnsWidth - COUNT_COLUMN_WIDTH - COLUMN_PAD;
        TooltipMakerAPI headerElement = panel.createUIElement(searchWidth, COLUMN_HEADING_Y, false);
        searchField = headerElement.addTextField(searchWidth, BUTTON_SIZE, Fonts.DEFAULT_SMALL, 0f);
        searchField.setUndoOnEscape(false);
        searchField.getPosition().setXAlignOffset(0f);
        searchField.setText(lastSearch);
        query = lastSearch.trim().toLowerCase();
        panel.addUIElement(headerElement).inTL(0f, 0f);

        searchLabel = Global.getSettings().createLabel("Search", Fonts.DEFAULT_SMALL);
        searchLabel.setColor(Misc.getGrayColor());
        searchLabel.getPosition().setSize(searchWidth - SEARCH_TEXT_PAD, BUTTON_SIZE);

        addColumnHeading("Mod", 0f, MOD_COLUMN_WIDTH);
        addColumnHeading("Patch", MOD_COLUMN_WIDTH + COLUMN_PAD, searchWidth - MOD_COLUMN_WIDTH - COLUMN_PAD);
        addColumnHeading("Sites", columnsWidth - COUNT_COLUMN_WIDTH, COUNT_COLUMN_WIDTH);

        recreatePanel();
    }

    private void addColumnHeading(String text, float x, float columnWidth) {
        TooltipMakerAPI element = panel.createUIElement(columnWidth, COLUMN_HEADING_HEIGHT, false);
        element.addSectionHeading(text, Misc.getBasePlayerColor(), Misc.getDarkPlayerColor(), Alignment.MID, columnWidth - COLUMN_HEADING_OVERHANG, 0f);
        panel.addUIElement(element).inTL(x, COLUMN_HEADING_Y);
    }

    public PositionAPI getPosition() {
        return panel.getPosition();
    }

    private static String getKey(PatchHandlerSpec handler) {
        return handler.handlerClassName() + "#" + handler.handlerMethodName();
    }

    private static String getSimpleName(String className) {
        return className.substring(className.lastIndexOf('.') + 1);
    }

    private static String getFullSiteName(PatchSiteRegistry.Site site) {
        return site.className() + "." + site.methodName();
    }

    private static String getModName(PatchHandlerSpec handler) {
        return handler.sourceMod().getName().trim();
    }

    private static boolean isPatchLib(PatchHandlerSpec handler) {
        return PATCHLIB_ID.equals(handler.sourceMod().getId());
    }

    /** PatchLib last, as its the least relevant mod. */
    private static Map<String, List<PatchHandlerSpec>> groupHandlers() {
        List<PatchHandlerSpec> handlers = PatchSiteRegistry.getHandlers();
        handlers.sort(Comparator.comparing(PatchListComponent::isPatchLib)
                .thenComparing(PatchListComponent::getModName)
                .thenComparing(handler -> getSimpleName(handler.handlerClassName()))
                .thenComparing(PatchHandlerSpec::handlerMethodName));

        Map<String, List<PatchHandlerSpec>> grouped = new LinkedHashMap<>();
        for (PatchHandlerSpec handler : handlers) {
            grouped.computeIfAbsent(handler.handlerClassName(), key -> new ArrayList<>()).add(handler);
        }
        return grouped;
    }

    private boolean matches(String text) {
        return !query.isEmpty() && text.toLowerCase().contains(query);
    }

    private boolean anySiteMatches(PatchHandlerSpec handler) {
        for (PatchSiteRegistry.Site site : PatchSiteRegistry.getSites(handler)) {
            if (matches(getFullSiteName(site))) return true;
        }
        return false;
    }

    private boolean handlerMatches(PatchHandlerSpec handler) {
        return matches(handler.handlerMethodName()) || anySiteMatches(handler);
    }

    private boolean anyHandlerMatches(List<PatchHandlerSpec> handlers) {
        for (PatchHandlerSpec handler : handlers) {
            if (handlerMatches(handler)) return true;
        }
        return false;
    }

    private boolean isPatchVisible(String className, List<PatchHandlerSpec> handlers) {
        return query.isEmpty() || matches(className) || matches(getModName(handlers.get(0))) || anyHandlerMatches(handlers);
    }

    private void recreatePanel() {
        if (subpanel != null) {
            lastScroller = subelement.getExternalScroller().getYOffset();
            panel.removeComponent(subpanel);
        }

        float listHeight = height - LIST_Y;
        subpanel = panel.createCustomPanel(width, listHeight, null);
        panel.addComponent(subpanel).inTL(0f, LIST_Y);
        subelement = subpanel.createUIElement(width, listHeight, true);

        boolean empty = true;
        for (Map.Entry<String, List<PatchHandlerSpec>> patch : groupHandlers().entrySet()) {
            if (!isPatchVisible(patch.getKey(), patch.getValue())) continue;
            addPatchRow(patch.getKey(), patch.getValue(), empty ? 0f : PATCH_PAD);
            empty = false;
        }

        if (empty) subelement.addPara("No matches", Misc.getGrayColor(), 0f);

        subpanel.addUIElement(subelement).inTL(0f, 0f);
        float maxScroller = Math.max(0f, subelement.getHeightSoFar() - listHeight);
        subelement.getExternalScroller().setYOffset(Math.min(lastScroller, maxScroller));
    }

    private void addPatchRow(String className, List<PatchHandlerSpec> handlers, float pad) {
        int siteCount = 0;
        for (PatchHandlerSpec handler : handlers) {
            siteCount += PatchSiteRegistry.getSites(handler).size();
        }

        CustomPanelAPI rowPanel = addRowPanel(pad);

        ModSpecAPI mod = handlers.get(0).sourceMod();
        addIcon(rowPanel, mod);

        float checkboxX = MOD_COLUMN_WIDTH + COLUMN_PAD - LIST_X_OFFSET;
        CheckboxElement checkbox = new CheckboxElement(rowPanel, PatchStateRegistry.isEnabled(className), isPatchLib(handlers.get(0)));
        checkbox.getPosition().inLMid(checkboxX);
        checkbox.onClick(event -> PatchStateRegistry.setEnabled(className, checkbox.isChecked()));

        float indent = checkboxX + CheckboxElement.SIZE;
        boolean childMatches = anyHandlerMatches(handlers);
        boolean expanded = childMatches || expandedKeys.contains(className);
        ListRowElement row = addRow(rowPanel, getSimpleName(className), String.valueOf(siteCount), Misc.getBasePlayerColor(), indent, true, expanded);
        row.onClick(event -> {
            if (event.isLMBEvent()) toggleExpanded(className);
        });
        addTooltip(row, "Patch", "Class name:", className, "Handlers:", String.valueOf(handlers.size()));

        if (!expanded) return;
        for (PatchHandlerSpec handler : handlers) {
            if (!childMatches || handlerMatches(handler)) addHandlerRow(handler, indent + INDENT);
        }
    }

    private void addIcon(CustomPanelAPI rowPanel, ModSpecAPI mod) {
        SpriteAPI sprite = Global.getSettings().getSprite(modIcons.computeIfAbsent(mod.getId(), id -> findIcon(mod)));
        sprite.setSize(ICON_SIZE, ICON_SIZE);

        BaseElement icon = new BaseElement(rowPanel, ICON_SIZE, ICON_SIZE);
        icon.getPosition().inLMid((MOD_COLUMN_WIDTH - ICON_SIZE) / 2f - LIST_X_OFFSET);
        icon.onRender(alphaMult -> {
            sprite.setAlphaMult(alphaMult);
            sprite.render(icon.getPosition().getX(), icon.getPosition().getY());
        });
        addTooltip(icon, "Mod", "Name:", mod.getName().trim(), "ID:", mod.getId(), "Version:", mod.getVersion());
    }

    /** Either the icon from a mods LunaLib settings config, or the icon.png in its root folder that TriOS uses. Mods with neither use the codex icon for mod content. */
    private static String findIcon(ModSpecAPI mod) {
        String lunaIcon = getLunaIcon(mod.getId());
        if (lunaIcon != null && loadTexture(lunaIcon)) return lunaIcon;

        //All icons are named the same, so this needs the full path, as relative ones resolve to the first mod that has the file.
        String rootIcon = mod.getPath() + "/icon.png";
        return loadTexture(rootIcon) ? rootIcon : Global.getSettings().getSpriteName("codex", "manual_other");
    }

    private static String getLunaIcon(String modId) {
        try {
            return Global.getSettings().loadJSON(LUNA_CONFIG, modId).getJSONObject(modId).getString("iconPath");
        } catch (Exception ex) {
            return null;
        }
    }

    private static boolean loadTexture(String path) {
        try {
            Global.getSettings().loadTexture(path);
            return true;
        } catch (Exception ex) {
            return false;
        }
    }

    private void addHandlerRow(PatchHandlerSpec handler, float indent) {
        List<PatchSiteRegistry.Site> sites = PatchSiteRegistry.getSites(handler);
        String key = getKey(handler);
        boolean childMatches = anySiteMatches(handler);
        boolean expanded = childMatches || expandedKeys.contains(key);

        Color color = Misc.scaleColorOnly(Misc.getBasePlayerColor(), HANDLER_BRIGHTNESS);
        ListRowElement row = addRow(addRowPanel(0f), handler.handlerMethodName(), String.valueOf(sites.size()), color, indent, !sites.isEmpty(), expanded);
        row.onClick(event -> {
            if (event.isLMBEvent() && !sites.isEmpty()) toggleExpanded(key);
        });
        addTooltip(row, "Handler", "Method:", handler.handlerMethodName(), "Type:", handler.getAnnotationName(), "Order:", String.valueOf(handler.order()));

        if (!expanded) return;
        Color siteColor = Misc.scaleColorOnly(Misc.getBasePlayerColor(), SITE_BRIGHTNESS);
        for (PatchSiteRegistry.Site site : sites) {
            if (childMatches && !matches(getFullSiteName(site))) continue;
            String siteText = getSimpleName(site.className()) + "." + site.methodName();
            ListRowElement siteRow = addRow(addRowPanel(0f), siteText, null, siteColor, indent + INDENT, false, false);
            addTooltip(siteRow, "Site", "Class name:", site.className(), "Method:", site.methodName(), "Descriptor:", site.descriptor());
        }
    }

    private CustomPanelAPI addRowPanel(float pad) {
        CustomPanelAPI rowPanel = Global.getSettings().createCustom(rowWidth, ROW_HEIGHT, null);
        subelement.addCustom(rowPanel, pad);
        return rowPanel;
    }

    private ListRowElement addRow(CustomPanelAPI rowPanel, String text, String count, Color color, float indent, boolean expandable, boolean expanded) {
        ListRowElement row = new ListRowElement(rowPanel, text, count, COUNT_COLUMN_WIDTH, color, rowWidth - indent, ROW_HEIGHT, expandable, expanded);
        row.getPosition().inLMid(indent);
        return row;
    }

    private void addTooltip(BaseElement element, String heading, String... labelsAndValues) {
        subelement.addTooltipTo(new BaseTooltipCreator() {
            @Override
            public float getTooltipWidth(Object tooltipParam) {
                return TOOLTIP_WIDTH;
            }

            @Override
            public void createTooltip(TooltipMakerAPI tooltip, boolean expanded, Object tooltipParam) {
                tooltip.addSectionHeading(heading, Misc.getBasePlayerColor(), Misc.getDarkPlayerColor(), Alignment.MID, 0f);

                Color[] colors = { Misc.getHighlightColor(), Misc.getTextColor() };
                for (int i = 0; i < labelsAndValues.length; i += 2) {
                    tooltip.addPara("%s %s", TOOLTIP_LINE_PAD, colors, labelsAndValues[i], labelsAndValues[i + 1]);
                }
            }
        }, element.getElementPanel(), TooltipMakerAPI.TooltipLocation.RIGHT, false);
    }

    private void toggleExpanded(String key) {
        Global.getSoundPlayer().playUISound("ui_button_pressed", 1f, 1f);
        if (!expandedKeys.remove(key)) expandedKeys.add(key);
        needsRecreate = true;
    }

    @Override
    public void render(float alphaMult) {
        if (!searchField.getText().isEmpty() || searchField.hasFocus()) return;

        PositionAPI position = searchField.getPosition();
        searchLabel.getPosition().setLocation(position.getX() + SEARCH_TEXT_PAD, position.getY());
        searchLabel.render(alphaMult);
    }

    @Override
    public void advance(float amount) {
        lastSearch = searchField.getText();
        String text = lastSearch.trim().toLowerCase();
        boolean queryChanged = !text.equals(query);
        query = text;

        if (!needsRecreate && !queryChanged) return;
        needsRecreate = false;
        recreatePanel();

        if (queryChanged) subelement.getExternalScroller().setYOffset(0f);
    }
}
