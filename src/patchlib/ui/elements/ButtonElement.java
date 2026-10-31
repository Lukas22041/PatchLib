package patchlib.ui.elements;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.graphics.SpriteAPI;
import com.fs.starfarer.api.ui.Alignment;
import com.fs.starfarer.api.ui.CutStyle;
import com.fs.starfarer.api.ui.Fonts;
import com.fs.starfarer.api.ui.LabelAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.ui.UIPanelAPI;
import com.fs.starfarer.api.util.FaderUtil;
import com.fs.starfarer.api.util.Misc;
import org.lwjgl.opengl.GL11;

import java.awt.Color;

public class ButtonElement extends BaseElement {

    private static final Color CLEAR = new Color(0, 0, 0, 0);

    private final Color fillColor = Global.getSettings().getColor("buttonBgDark");
    private final Color textColor = Global.getSettings().getColor("buttonText");
    private final FaderUtil glow = new FaderUtil(0f, 0.05f, 0.25f);
    private final SpriteAPI scanlines = Global.getSettings().getSprite("ui", "scanline11");
    private final CutStyle cutStyle;
    private final LabelAPI label;

    public ButtonElement(UIPanelAPI parent, String text, float width, float height, CutStyle cutStyle) {
        super(parent, width, height);
        this.cutStyle = cutStyle;

        scanlines.setColor(Color.black);

        //Button text has no outline or shadow, which the API can not turn off. Titles are the closest, as unlike other labels they only have a small shadow.
        TooltipMakerAPI element = elementPanel.createUIElement(width, height, false);
        element.setTitleFont(Fonts.ORBITRON_12);
        element.setTitleFontColor(textColor);
        label = element.addTitle(text);
        label.setAlignment(Alignment.MID);
        label.getPosition().setSize(width, height).inTL(0f, 0f).setXAlignOffset(0f);
        elementPanel.addUIElement(element).inTL(0f, 0f);

        onHoverEnter(event -> {
            Global.getSoundPlayer().playUISound("ui_button_mouseover", 1f, 1f);
            glow.fadeIn();
        });
        onHoverExit(event -> glow.fadeOut());
    }

    @Override
    public void advance(float amount) {
        glow.advance(amount);
        super.advance(amount);
    }

    @Override
    public void renderBelow(float alphaMult) {
        float brightness = glow.getBrightness();

        renderFill(fillColor, alphaMult, false);
        if (brightness > 0f) {
            float alpha = alphaMult * brightness * 0.5f * Math.min(1f, (fillColor.getAlpha() + 50f) / 255f);
            renderFill(Misc.interpolateColor(textColor, Color.white, brightness * 0.5f), alpha, true);
        }

        label.setColor(Misc.interpolateColor(textColor, Color.white, brightness));

        super.renderBelow(alphaMult);
    }

    private void renderFill(Color color, float alpha, boolean additive) {
        float x = getPosition().getX();
        float y = getPosition().getY();
        float width = getPosition().getWidth();
        float height = getPosition().getHeight();
        float tilesX = width / scanlines.getWidth();
        float tilesY = height / scanlines.getHeight();

        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glColorMask(false, false, false, true);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ZERO);
        Misc.renderQuad(x - 1f, y - 1f, width + 2f, height + 2f, CLEAR, 1f);
        GL11.glColor4ub((byte) 0, (byte) 0, (byte) 0, (byte) (255f * alpha));
        renderShape(x, y, width, height);

        scanlines.setBlendFunc(GL11.GL_ZERO, GL11.GL_SRC_ALPHA);
        scanlines.renderRegion(x, y, 0f, 0f, tilesX, tilesY);
        GL11.glColorMask(true, true, true, false);
        scanlines.setBlendFunc(GL11.GL_DST_ALPHA, GL11.GL_ONE_MINUS_DST_ALPHA);
        scanlines.renderRegion(x, y, 0f, 0f, tilesX, tilesY);

        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, additive ? GL11.GL_ONE : GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glColor4ub((byte) color.getRed(), (byte) color.getGreen(), (byte) color.getBlue(), (byte) (color.getAlpha() * alpha));
        renderShape(x, y, width, height);
        GL11.glEnable(GL11.GL_TEXTURE_2D);
    }

    private void renderShape(float x, float y, float width, float height) {
        float cut = Math.round(height * (cutStyle == CutStyle.ALL || cutStyle == CutStyle.NONE ? 0.25f : 0.33333f));

        GL11.glBegin(GL11.GL_TRIANGLE_STRIP);
        switch (cutStyle) {
            case ALL -> {
                GL11.glVertex2f(x + width - cut, y);
                GL11.glVertex2f(x + cut, y);
                GL11.glVertex2f(x + width, y + cut - 1f);
                GL11.glVertex2f(x, y + cut - 1f);
                GL11.glVertex2f(x + width, y + height - cut);
                GL11.glVertex2f(x, y + height - cut);
                GL11.glVertex2f(x + width - cut, y + height);
                GL11.glVertex2f(x + cut, y + height);
            }
            case TL_BR -> {
                GL11.glVertex2f(x, y);
                GL11.glVertex2f(x, y + height - cut);
                GL11.glVertex2f(x + width - cut, y);
                GL11.glVertex2f(x + cut, y + height);
                GL11.glVertex2f(x + width, y + cut);
                GL11.glVertex2f(x + width, y + height);
            }
            case BL_TR -> {
                GL11.glVertex2f(x + width, y);
                GL11.glVertex2f(x + cut, y);
                GL11.glVertex2f(x + width, y + height - cut);
                GL11.glVertex2f(x, y + cut);
                GL11.glVertex2f(x + width - cut, y + height);
                GL11.glVertex2f(x, y + height);
            }
            case TOP -> {
                GL11.glVertex2f(x, y);
                GL11.glVertex2f(x + width, y);
                GL11.glVertex2f(x, y + height - cut);
                GL11.glVertex2f(x + width, y + height - cut);
                GL11.glVertex2f(x + cut, y + height);
                GL11.glVertex2f(x + width - cut, y + height);
            }
            case BOTTOM -> {
                GL11.glVertex2f(x + cut, y);
                GL11.glVertex2f(x + width - cut, y);
                GL11.glVertex2f(x, y + cut);
                GL11.glVertex2f(x + width, y + cut);
                GL11.glVertex2f(x, y + height);
                GL11.glVertex2f(x + width, y + height);
            }
            case C2_MENU -> {
                GL11.glVertex2f(x + width, y);
                GL11.glVertex2f(x + cut * 0.75f, y);
                GL11.glVertex2f(x + width, y + height - cut * 1.5f);
                GL11.glVertex2f(x, y + cut * 0.75f);
                GL11.glVertex2f(x + width - cut * 1.5f, y + height);
                GL11.glVertex2f(x, y + height);
            }
            case NONE -> {
                GL11.glVertex2f(x + width, y);
                GL11.glVertex2f(x, y);
                GL11.glVertex2f(x + width, y + height);
                GL11.glVertex2f(x, y + height);
            }
        }
        GL11.glEnd();
    }
}
