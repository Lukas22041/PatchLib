package patchlib.ui.components;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.BaseCustomUIPanelPlugin;
import com.fs.starfarer.api.ui.CustomPanelAPI;
import com.fs.starfarer.api.ui.CutStyle;
import com.fs.starfarer.api.ui.Fonts;
import com.fs.starfarer.api.ui.LabelAPI;
import com.fs.starfarer.api.ui.PositionAPI;
import com.fs.starfarer.api.ui.ScrollPanelAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.ui.UIComponentAPI;
import com.fs.starfarer.api.ui.UIPanelAPI;
import com.fs.starfarer.api.util.Misc;
import org.apache.log4j.Level;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;
import patchlib.agent.log.LogBuffer;
import patchlib.ui.elements.ButtonElement;

import java.awt.Color;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/** Tabbed view of the most recent log entries, which keeps updating while it is open. */
public class LogComponent extends BaseCustomUIPanelPlugin {

    private enum Tab {
        PATCHLIB("PatchLib Log", LogBuffer.PATCHLIB),
        GAME("Game Log", LogBuffer.GAME);

        private final String title;
        private final LogBuffer.Channel channel;

        Tab(String title, LogBuffer.Channel channel) {
            this.title = title;
            this.channel = channel;
        }
    }

    private record Row(LabelAPI label, float height) { }

    private static final float TEXT_BRIGHTNESS = 0.92f;

    private static final float TAB_WIDTH = 130f;
    private static final float TAB_HEIGHT = 18f;
    private static final float TAB_PAD = 1f;
    private static final float BOX_PAD = 10f;
    private static final float TEXT_RIGHT_PAD = 10f;
    private static final float ROW_PAD = 2f;
    private static final float RECREATE_INTERVAL = 0.25f;
    private static final float BOTTOM_TOLERANCE = 5f;

    //Creating all labels at once hitches, so only what fills the list is created right away, and the older entries over the next frames.
    private static final int INITIAL_ROWS = 100;
    private static final int BACKLOG_ROWS_PER_FRAME = 50;

    private static Tab currentTab = Tab.PATCHLIB;

    private final CustomPanelAPI panel;
    private final Map<Tab, ButtonElement> tabButtons = new EnumMap<>(Tab.class);
    private final Deque<Row> rows = new ArrayDeque<>();
    private final Deque<LogBuffer.Entry> backlog = new ArrayDeque<>();
    private final float width;
    private final float height;
    private final float listWidth;
    private final float listHeight;
    private final float textWidth;

    private CustomPanelAPI subpanel;
    private TooltipMakerAPI subelement;
    private PositionAPI position;
    private long lastIndex = 0;
    private float contentHeight = 0f;
    private float sinceRecreate = 0f;
    private boolean needsTabChange = false;

    public LogComponent(UIPanelAPI parent, float width, float height) {
        this.width = width;
        this.height = height;
        this.listWidth = width - BOX_PAD - 1f; //Places the scrollbar right next to the boxes border.
        this.listHeight = height - TAB_HEIGHT - BOX_PAD * 2f;
        this.textWidth = listWidth - TEXT_RIGHT_PAD;

        panel = Global.getSettings().createCustom(width, height, this);
        parent.addComponent(panel);

        float tabX = 0f;
        for (Tab tab : Tab.values()) {
            ButtonElement button = new ButtonElement(panel, tab.title, TAB_WIDTH, TAB_HEIGHT, CutStyle.TOP);
            button.getPosition().inTL(tabX, 0f);
            button.onPressed(() -> {
                if (tab == currentTab) return;
                currentTab = tab;
                needsTabChange = true;
            });
            tabButtons.put(tab, button);
            tabX += TAB_WIDTH + TAB_PAD;
        }

        showCurrentTab();
    }

    public PositionAPI getPosition() {
        return panel.getPosition();
    }

    private void showCurrentTab() {
        for (Map.Entry<Tab, ButtonElement> button : tabButtons.entrySet()) {
            button.getValue().setHighlighted(button.getKey() == currentTab);
        }

        rows.clear();
        backlog.clear();
        contentHeight = 0f;
        lastIndex = 0;

        List<LogBuffer.Entry> entries = currentTab.channel.getEntriesAfter(lastIndex);
        if (!entries.isEmpty()) lastIndex = entries.get(entries.size() - 1).index();

        int backlogSize = Math.max(0, entries.size() - INITIAL_ROWS);
        backlog.addAll(entries.subList(0, backlogSize));
        for (LogBuffer.Entry entry : entries.subList(backlogSize, entries.size())) {
            addRow(entry);
        }

        recreatePanel(getMaxScroller());
    }

    private void addRow(LogBuffer.Entry entry) {
        Row row = createRow(entry);
        rows.addLast(row);
        contentHeight += row.height();
    }

    /** Returns the height that was added above the existing rows. */
    private float readBacklog() {
        float added = 0f;
        for (int i = 0; i < BACKLOG_ROWS_PER_FRAME && !backlog.isEmpty(); i++) {
            Row row = createRow(backlog.removeLast());
            rows.addFirst(row);
            added += row.height();
        }
        contentHeight += added;
        return added;
    }

    /** Returns the height of the rows that got dropped to make room for the new ones. */
    private float readNewEntries() {
        List<LogBuffer.Entry> entries = currentTab.channel.getEntriesAfter(lastIndex);
        if (entries.isEmpty()) return 0f;

        lastIndex = entries.get(entries.size() - 1).index();
        for (LogBuffer.Entry entry : entries) {
            addRow(entry);
        }

        float dropped = 0f;
        while (rows.size() + backlog.size() > LogBuffer.MAX_ENTRIES) {
            if (backlog.isEmpty()) dropped += rows.removeFirst().height();
            else backlog.removeFirst();
        }
        contentHeight -= dropped;
        return dropped;
    }

    private Row createRow(LogBuffer.Entry entry) {
        LabelAPI label = Global.getSettings().createLabel(entry.loggerName() + " - " + entry.text(), Fonts.DEFAULT_SMALL);
        label.setColor(getColor(entry.level()));
        label.setHighlightColor(Misc.getBasePlayerColor());
        label.highlightFirst(entry.loggerName());
        return new Row(label, label.autoSizeToWidth(textWidth).getHeight() + ROW_PAD);
    }

    private static Color getColor(Level level) {
        Color color = Misc.getTextColor();
        if (level.isGreaterOrEqual(Level.ERROR)) color = Misc.getNegativeHighlightColor();
        else if (level.isGreaterOrEqual(Level.WARN)) color = Misc.getHighlightColor();
        return Misc.scaleColorOnly(color, TEXT_BRIGHTNESS);
    }

    private float getMaxScroller() {
        return Math.max(0f, contentHeight - listHeight);
    }

    private void recreatePanel(float scroller) {
        if (subpanel != null) panel.removeComponent(subpanel);

        subpanel = panel.createCustomPanel(listWidth, listHeight, null);
        panel.addComponent(subpanel).inTL(BOX_PAD, TAB_HEIGHT + BOX_PAD);
        subelement = subpanel.createUIElement(listWidth, listHeight, true);
        subelement.addSpacer(contentHeight);
        subpanel.addUIElement(subelement).inTL(0f, 0f);

        //The scroller only renders children that are in view, but a tooltip counts as a single child, which is why the labels are not placed in to it.
        ScrollPanelAPI externalScroller = subelement.getExternalScroller();
        float y = 0f;
        for (Row row : rows) {
            externalScroller.addComponent((UIComponentAPI) row.label()).inTL(0f, y);
            y += row.height();
        }

        externalScroller.setYOffset(Math.max(0f, Math.min(scroller, getMaxScroller())));
    }

    @Override
    public void positionChanged(PositionAPI position) {
        this.position = position;
    }

    @Override
    public void renderBelow(float alphaMult) {
        if (position == null) return;

        float x = position.getX();
        float y = position.getY();
        float boxHeight = height - TAB_HEIGHT;
        Color color = Misc.getDarkPlayerColor();

        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        Misc.renderQuad(x, y, width, 1f, color, alphaMult);
        Misc.renderQuad(x, y + boxHeight - 1f, width, 1f, color, alphaMult);
        Misc.renderQuad(x, y, 1f, boxHeight, color, alphaMult);
        Misc.renderQuad(x + width - 1f, y, 1f, boxHeight, color, alphaMult);
        GL11.glEnable(GL11.GL_TEXTURE_2D);
    }

    @Override
    public void advance(float amount) {
        if (needsTabChange) {
            needsTabChange = false;
            showCurrentTab();
            return;
        }

        sinceRecreate += amount;

        //Recreating drops the scrollers input state, which would end a drag of its thumb.
        boolean loadingBacklog = !backlog.isEmpty();
        if ((sinceRecreate < RECREATE_INTERVAL && !loadingBacklog) || Mouse.isButtonDown(0)) return;

        float scroller = subelement.getExternalScroller().getYOffset();
        boolean atBottom = scroller >= getMaxScroller() - BOTTOM_TOLERANCE;
        long previousIndex = lastIndex;

        float heightAbove = readBacklog() - readNewEntries();
        if (lastIndex == previousIndex && !loadingBacklog) return;

        sinceRecreate = 0f;
        recreatePanel(atBottom ? getMaxScroller() : scroller + heightAbove);
    }
}
