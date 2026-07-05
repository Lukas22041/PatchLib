package patchlib.debug.ui;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.BaseCustomUIPanelPlugin;
import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.ui.PositionAPI;
import com.fs.starfarer.api.util.FaderUtil;

import java.awt.Color;
import java.util.List;

/** Backs one row's custom panel inside the vanilla scroller. Every row is drawn the same way: this plugin
 *  fills the row background, brightens it on hover, and plays the stock mouseover sound. Class/method rows
 *  also carry a click id (the whole row acts as a button, firing on release with a click sound) and an
 *  open/close caret; leaf rows carry neither. The column text sits in LabelAPI children placed by the window. */
public class PatchRow extends BaseCustomUIPanelPlugin {

    public interface Listener {
        void onRowClicked(String id);
    }

    private static final float CARET_SIZE = 3.5f;

    private final Color bgColor;
    private final float bgAlpha;
    private final float bgInset;
    private final Color accent;
    private final Boolean expanded; // null on leaf rows: no caret
    private final float caretX;
    private final String clickId;   // null on non-clickable rows
    private final Listener listener;

    private PositionAPI pos;
    private boolean hover;
    private boolean pressed;
    private final FaderUtil hoverFader = new FaderUtil(0f, 0.08f, 0.2f);

    public PatchRow(Color bgColor, float bgAlpha, float bgInset, Color accent,
                    Boolean expanded, float caretX, String clickId, Listener listener) {
        this.bgColor = bgColor;
        this.bgAlpha = bgAlpha;
        this.bgInset = bgInset;
        this.accent = accent;
        this.expanded = expanded;
        this.caretX = caretX;
        this.clickId = clickId;
        this.listener = listener;
    }

    @Override
    public void positionChanged(PositionAPI position) {
        this.pos = position;
    }

    @Override
    public void advance(float amount) {
        if (hover) hoverFader.fadeIn(); else hoverFader.fadeOut();
        hoverFader.advance(amount);
    }

    @Override
    public void renderBelow(float alphaMult) {
        if (pos == null) return;
        float x = pos.getX() + bgInset;
        float bw = pos.getWidth() - bgInset;
        if (bgAlpha > 0f) DebugRender.fillRect(x, pos.getY(), bw, pos.getHeight(), bgColor, bgAlpha * alphaMult);
        float glow = hoverFader.getBrightness();
        if (glow > 0f) DebugRender.fillRect(x, pos.getY(), bw, pos.getHeight(), accent, 0.14f * glow * alphaMult);
    }

    @Override
    public void render(float alphaMult) {
        if (pos == null || expanded == null) return;
        DebugRender.triangle(pos.getX() + caretX, pos.getY() + pos.getHeight() * 0.5f, CARET_SIZE, expanded, accent, 0.9f * alphaMult);
    }

    @Override
    public void processInput(List<InputEventAPI> events) {
        if (pos == null) return;
        for (InputEventAPI event : events) {
            if (event.isConsumed()) continue;
            if (event.isMouseMoveEvent()) {
                boolean now = pos.containsEvent(event);
                if (now && !hover) playUISound("ui_button_mouseover");
                hover = now;
            } else if (clickId != null && event.isLMBDownEvent() && pos.containsEvent(event)) {
                pressed = true;
                event.consume();
            } else if (clickId != null && event.isLMBUpEvent()) {
                if (pressed && pos.containsEvent(event) && listener != null) {
                    playUISound("ui_button_pressed");
                    event.consume();
                    listener.onRowClicked(clickId);
                }
                pressed = false;
            }
        }
    }

    private static void playUISound(String id) {
        try { Global.getSoundPlayer().playUISound(id, 1f, 0.5f); } catch (Exception e) { }
    }
}
