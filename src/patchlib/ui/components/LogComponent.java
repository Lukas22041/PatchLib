package patchlib.ui.components;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.BaseCustomUIPanelPlugin;
import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.ui.CustomPanelAPI;
import com.fs.starfarer.api.ui.CutStyle;
import com.fs.starfarer.api.ui.Fonts;
import com.fs.starfarer.api.ui.LabelAPI;
import com.fs.starfarer.api.ui.PositionAPI;
import com.fs.starfarer.api.ui.ScrollPanelAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.ui.UIComponentAPI;
import com.fs.starfarer.api.ui.UIPanelAPI;
import com.fs.starfarer.api.util.FaderUtil;
import com.fs.starfarer.api.util.Misc;
import org.apache.log4j.Level;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;
import patchlib.agent.log.LogBuffer;
import patchlib.ui.elements.BoxElement;
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

    private record Row(LabelAPI label, float width, float height) { }

    private static final float TEXT_BRIGHTNESS = 0.92f;

    private static final float TAB_WIDTH = 130f;
    private static final float TAB_HEIGHT = 18f;
    private static final float TAB_PAD = 1f;
    private static final float BOX_PAD = 10f;
    private static final float TEXT_RIGHT_PAD = 10f;
    private static final float ROW_PAD = 2f;
    private static final float SCROLLER_CONTENT_INSET = 5f;
    private static final float MAX_WIDTH_MULT = 6f;
    private static final float HORIZONTAL_SCROLL_STEP = 100f;
    private static final float SCROLL_EASING = 3f;
    private static final float SCROLL_MIN_SPEED = 200f;
    private static final float RAIL_HEIGHT = 4f;
    private static final float RAIL_PREVIEW_ALPHA = 0.5f;
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
    private final FaderUtil railPreview = new FaderUtil(0f, 0.1f, 0.1f);
    private final float listWidth;
    private final float listHeight;
    private final float maxTextWidth;

    private CustomPanelAPI subpanel;
    private TooltipMakerAPI subelement;
    private long lastIndex = 0;
    private float contentHeight = 0f;
    private float minXOffset = 0f;
    private float targetXOffset = 0f;
    private boolean scrollingX = false;
    private boolean overRail = false;
    private boolean draggingRail = false;
    private float sinceRecreate = 0f;
    private boolean needsTabChange = false;

    public LogComponent(UIPanelAPI parent, float width, float height) {
        //Places both scrollbars right next to the boxes border.
        this.listWidth = width - BOX_PAD - 1f;
        this.listHeight = height - TAB_HEIGHT - BOX_PAD - 1f;
        this.maxTextWidth = listWidth * MAX_WIDTH_MULT;

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

        new BoxElement(panel, width, height - TAB_HEIGHT).getPosition().inTL(0f, TAB_HEIGHT);

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
        scrollingX = false;

        List<LogBuffer.Entry> entries = currentTab.channel.getEntriesAfter(lastIndex);
        if (!entries.isEmpty()) lastIndex = entries.get(entries.size() - 1).index();

        int backlogSize = Math.max(0, entries.size() - INITIAL_ROWS);
        backlog.addAll(entries.subList(0, backlogSize));
        for (LogBuffer.Entry entry : entries.subList(backlogSize, entries.size())) {
            addRow(entry);
        }

        recreatePanel(0f, getMaxScroller());
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
        String text = entry.loggerName() + " - " + entry.text();

        LabelAPI label = Global.getSettings().createLabel(text, Fonts.DEFAULT_SMALL);
        label.setColor(getColor(entry.level()));
        label.setHighlightColor(Misc.getBasePlayerColor());
        label.highlightFirst(entry.loggerName());

        float width = 0f;
        for (String line : text.split("\n")) {
            width = Math.max(width, label.computeTextWidth(line));
        }
        return new Row(label, Math.min(width, maxTextWidth), label.autoSizeToWidth(maxTextWidth).getHeight() + ROW_PAD);
    }

    private static Color getColor(Level level) {
        Color color = Misc.getTextColor();
        if (level.isGreaterOrEqual(Level.ERROR)) color = Misc.getNegativeHighlightColor();
        else if (level.isGreaterOrEqual(Level.WARN)) color = Misc.getHighlightColor();
        return Misc.scaleColorOnly(color, TEXT_BRIGHTNESS);
    }

    private float getMaxScroller() {
        return Math.max(0f, contentHeight + BOX_PAD - listHeight);
    }

    private float clampXOffset(float xOffset) {
        return Math.max(minXOffset, Math.min(xOffset, 0f));
    }

    private void recreatePanel(float xOffset, float yOffset) {
        if (subpanel != null) panel.removeComponent(subpanel);

        float elementWidth = listWidth;
        for (Row row : rows) {
            elementWidth = Math.max(elementWidth, row.width() + TEXT_RIGHT_PAD + SCROLLER_CONTENT_INSET);
        }
        minXOffset = listWidth - (elementWidth - SCROLLER_CONTENT_INSET);

        subpanel = panel.createCustomPanel(listWidth, listHeight, null);
        panel.addComponent(subpanel).inTL(BOX_PAD, TAB_HEIGHT + BOX_PAD);
        subelement = subpanel.createUIElement(elementWidth, listHeight, true);
        subelement.addSpacer(contentHeight + BOX_PAD);
        //The scroller is sized to the element, shrinking it afterwards is what makes the stock horizontal scrollbar appear.
        subpanel.addUIElement(subelement).setSize(listWidth, listHeight).inTL(0f, 0f);
        addHorizontalScrollInput();

        //The scroller only renders children that are in view, but a tooltip counts as a single child, which is why the labels are not placed in to it.
        ScrollPanelAPI externalScroller = subelement.getExternalScroller();
        float y = 0f;
        for (Row row : rows) {
            externalScroller.addComponent((UIComponentAPI) row.label()).inTL(0f, y);
            y += row.height();
        }

        externalScroller.setXOffset(clampXOffset(xOffset));
        externalScroller.setYOffset(Math.max(0f, Math.min(yOffset, getMaxScroller())));
    }

    /** The scroller uses the wheel for its vertical scrolling, so this is added after it, as the last added component gets input first.
     * Its preview of the horizontal thumb is not limited to the rail and shows for any input on it, so this takes over all input on the rail and renders a limited preview instead. */
    private void addHorizontalScrollInput() {
        CustomPanelAPI inputPanel = Global.getSettings().createCustom(listWidth, listHeight, new BaseCustomUIPanelPlugin() {
            @Override
            public void processInput(List<InputEventAPI> events) {
                for (InputEventAPI event : events) {
                    if (event.isConsumed()) continue;

                    if (event.isMouseScrollEvent() && event.isShiftDown() && subpanel.getPosition().containsEvent(event)) {
                        float xOffset = scrollingX ? targetXOffset : subelement.getExternalScroller().getXOffset();
                        targetXOffset = clampXOffset(xOffset + HORIZONTAL_SCROLL_STEP * Math.signum(event.getEventValue()));
                        scrollingX = true;
                        event.consume();
                    } else if (!event.isMouseScrollEvent()) {
                        processRailInput(event);
                    }
                }
            }

            @Override
            public void render(float alphaMult) {
                renderRailPreview(alphaMult);
            }
        });
        subpanel.addComponent(inputPanel).inTL(0f, 0f);
    }

    private float getThumbWidth() {
        return listWidth * listWidth / (listWidth - minXOffset);
    }

    private float getThumbX() {
        float progress = -subelement.getExternalScroller().getXOffset() / (listWidth - minXOffset);
        return subpanel.getPosition().getX() + progress * listWidth;
    }

    private boolean isOverRail(InputEventAPI event) {
        PositionAPI list = subpanel.getPosition();
        return minXOffset < 0f && list.containsEvent(event) && event.getY() <= list.getY() + RAIL_HEIGHT;
    }

    private void processRailInput(InputEventAPI event) {
        if (!event.isMouseEvent()) return;

        ScrollPanelAPI scroller = subelement.getExternalScroller();
        float contentPerPixel = (listWidth - minXOffset) / listWidth;

        if (draggingRail) {
            if (event.isLMBUpEvent()) draggingRail = false;
            else if (event.isMouseMoveEvent()) scroller.setXOffset(clampXOffset(scroller.getXOffset() - event.getDX() * contentPerPixel));
            event.consume();
            return;
        }

        overRail = isOverRail(event);
        if (!overRail) return;

        if (event.isLMBDownEvent()) {
            draggingRail = true;
            scrollingX = false;

            //Clicking next to the thumb centers it on the click, same as with the vertical scrollbar.
            float x = event.getX();
            if (x < getThumbX() || x > getThumbX() + getThumbWidth()) {
                float thumbX = x - subpanel.getPosition().getX() - getThumbWidth() / 2f;
                scroller.setXOffset(clampXOffset(-thumbX * contentPerPixel));
            }
        }
        event.consume();
    }

    private void renderRailPreview(float alphaMult) {
        float brightness = railPreview.getBrightness();
        if (brightness <= 0f) return;

        PositionAPI list = subpanel.getPosition();
        float thumbWidth = getThumbWidth();
        float x = draggingRail ? getThumbX() : Global.getSettings().getMouseX() - thumbWidth / 2f;
        x = Math.max(list.getX(), Math.min(x, list.getX() + listWidth - thumbWidth));

        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        Misc.renderQuad(x, list.getY(), thumbWidth, RAIL_HEIGHT - 1f, Global.getSettings().getColor("buttonText"), alphaMult * brightness * RAIL_PREVIEW_ALPHA);
        GL11.glEnable(GL11.GL_TEXTURE_2D);
    }

    /** The API can only set the offset instantly, so this eases it with the same curve the scroller uses for the wheel. */
    private void advanceHorizontalScroll(float amount) {
        if (!scrollingX) return;

        ScrollPanelAPI scroller = subelement.getExternalScroller();
        float difference = clampXOffset(targetXOffset) - scroller.getXOffset();
        float step = Math.signum(difference) * amount * (Math.abs(difference * SCROLL_EASING) + SCROLL_MIN_SPEED);
        if (Math.abs(step) >= Math.abs(difference)) {
            step = difference;
            scrollingX = false;
        }
        scroller.setXOffset(scroller.getXOffset() + step);
    }

    @Override
    public void advance(float amount) {
        if (needsTabChange) {
            needsTabChange = false;
            showCurrentTab();
            return;
        }

        advanceHorizontalScroll(amount);

        if (overRail) railPreview.fadeIn();
        else railPreview.fadeOut();
        railPreview.advance(amount);

        sinceRecreate += amount;

        //Recreating drops the scrollers input state, which would end a drag of its thumb.
        boolean loadingBacklog = !backlog.isEmpty();
        if ((sinceRecreate < RECREATE_INTERVAL && !loadingBacklog) || Mouse.isButtonDown(0)) return;

        ScrollPanelAPI scroller = subelement.getExternalScroller();
        float xOffset = scroller.getXOffset();
        float yOffset = scroller.getYOffset();
        boolean atBottom = yOffset >= getMaxScroller() - BOTTOM_TOLERANCE;
        long previousIndex = lastIndex;

        float heightAbove = readBacklog() - readNewEntries();
        if (lastIndex == previousIndex && !loadingBacklog) return;

        sinceRecreate = 0f;
        recreatePanel(xOffset, atBottom ? getMaxScroller() : yOffset + heightAbove);
    }
}
