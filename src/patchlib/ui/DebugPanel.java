package patchlib.ui;

import com.fs.starfarer.api.GameState;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.BaseCustomUIPanelPlugin;
import com.fs.starfarer.api.graphics.SpriteAPI;
import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.ui.CustomPanelAPI;
import com.fs.starfarer.api.ui.PositionAPI;
import com.fs.starfarer.api.ui.UIPanelAPI;
import com.fs.starfarer.api.util.Misc;
import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.GL11;
import patchlib.ui.components.PatchListComponent;

import java.awt.Color;
import java.util.List;

/** Debug panel for toggling patches, showing PatchLibs log and showing buttons for starting JFR profiling*/
public class DebugPanel extends BaseCustomUIPanelPlugin {

    private static final float MAX_WIDTH = 1280f;
    private static final float MAX_HEIGHT = 900f;
    private static final float BACKGROUND_DIM = 0.66f;
    private static final float FILL_ALPHA = 0.8f;
    private static final float BACKGROUND_INSET = 3f;
    private static final float CONTENT_PAD = 20f;
    private static final float PATCH_LIST_WIDTH = 350f;
    private static final Color BACKGROUND_COLOR = new Color(0, 0, 0, 125);
    private static final Color BACKGROUND_OFFSET_COLOR = new Color(0, 0, 0, 175);

    private static DebugPanel current;

    private final SpriteAPI bottomLeft = getSprite("ui_border1_bot_left");
    private final SpriteAPI bottomRight = getSprite("ui_border1_bot_right");
    private final SpriteAPI topLeft = getSprite("ui_border1_top_left");
    private final SpriteAPI topRight = getSprite("ui_border1_top_right");
    private final SpriteAPI left = getSprite("ui_border1_left");
    private final SpriteAPI right = getSprite("ui_border1_right");
    private final SpriteAPI top = getSprite("ui_border1_top");
    private final SpriteAPI bottom = getSprite("ui_border1_bot");
    private final SpriteAPI scanlines = getSprite("scanline11");

    private final UIPanelAPI parent;
    private final boolean unpauseOnClose;
    private final CustomPanelAPI panel;
    private PositionAPI position;

    public static void toggle(UIPanelAPI parent) {
        if (current != null && current.parent == parent) {
            current.close();
        } else if (!Global.getSettings().isShowingCodex()) { //The codex renders above the screen panel and takes all input.
            current = new DebugPanel(parent);
        }
    }

    private static SpriteAPI getSprite(String key) {
        return Global.getSettings().getSprite("ui", key);
    }

    private DebugPanel(UIPanelAPI parent) {
        this.parent = parent;

        float width = Math.min(parent.getPosition().getWidth(), MAX_WIDTH);
        float height = Math.min(parent.getPosition().getHeight(), MAX_HEIGHT);

        panel = Global.getSettings().createCustom(width, height, this);
        parent.addComponent(panel).inMid();

        PatchListComponent patchList = new PatchListComponent(panel, PATCH_LIST_WIDTH, height - CONTENT_PAD * 2f);
        patchList.getPosition().inTL(CONTENT_PAD, CONTENT_PAD);

        unpauseOnClose = Global.getCurrentState() == GameState.CAMPAIGN && !Global.getSector().isPaused();
        if (unpauseOnClose) Global.getSector().setPaused(true);
    }

    private void close() {
        parent.removeComponent(panel);
        if (unpauseOnClose) Global.getSector().setPaused(false);
        current = null;
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
        float width = position.getWidth();
        float height = position.getHeight();

        renderDarkQuad(parent.getPosition(), alphaMult * BACKGROUND_DIM);
        renderDarkQuad(position, alphaMult * FILL_ALPHA);
        renderBorder(x, y, width, height, alphaMult);

        float backgroundX = x + BACKGROUND_INSET;
        float backgroundY = y + BACKGROUND_INSET;
        float backgroundWidth = width - BACKGROUND_INSET * 2f;
        float backgroundHeight = height - BACKGROUND_INSET * 2f;
        renderScanlines(backgroundX, backgroundY, backgroundWidth, backgroundHeight, BACKGROUND_COLOR, alphaMult);
        renderScanlines(backgroundX, backgroundY + 1f, backgroundWidth, backgroundHeight - 1f, BACKGROUND_OFFSET_COLOR, alphaMult);
    }

    private void renderDarkQuad(PositionAPI position, float alpha) {
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        Misc.renderQuad(position.getX(), position.getY(), position.getWidth(), position.getHeight(), Color.black, alpha);
        GL11.glEnable(GL11.GL_TEXTURE_2D);
    }

    private void renderBorder(float x, float y, float width, float height, float alphaMult) {
        for (SpriteAPI sprite : new SpriteAPI[]{ bottomLeft, bottomRight, topLeft, topRight, left, right, top, bottom }) {
            sprite.setAlphaMult(alphaMult);
        }

        float size = bottomLeft.getWidth();
        float tilesX = (width - size * 2f) / size;
        float tilesY = (height - size * 2f) / size;
        float rightX = x + width - size;
        float topY = y + height - size;

        bottomLeft.render(x, y);
        bottomRight.render(rightX, y);
        topLeft.render(x, topY);
        topRight.render(rightX, topY);
        left.renderRegion(x, y + size, 0f, 0f, 1f, tilesY);
        right.renderRegion(rightX, y + size, 0f, 0f, 1f, tilesY);
        top.renderRegion(x + size, topY, 0f, 0f, tilesX, 1f);
        bottom.renderRegion(x + size, y, 0f, 0f, tilesX, 1f);
    }

    private void renderScanlines(float x, float y, float width, float height, Color color, float alphaMult) {
        scanlines.setColor(color);
        scanlines.setAlphaMult(alphaMult);
        scanlines.renderRegion(x, y, 0f, 0f, width / scanlines.getWidth(), height / scanlines.getHeight());
    }

    @Override
    public void processInput(List<InputEventAPI> events) {
        boolean cancel = false;

        for (InputEventAPI event : events) {
            if (event.isConsumed()) continue;

            if (event.isKeyDownEvent() && event.getEventValue() == Keyboard.KEY_ESCAPE) cancel = true;
            if (event.isRMBDownEvent() && !panel.getPosition().containsEvent(event)) cancel = true;

            event.consume();
        }

        if (cancel) close();
    }
}
