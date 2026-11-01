package patchlib.ui.elements;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.ui.Alignment;
import com.fs.starfarer.api.ui.Fonts;
import com.fs.starfarer.api.ui.LabelAPI;
import com.fs.starfarer.api.ui.UIPanelAPI;
import com.fs.starfarer.api.util.FaderUtil;
import com.fs.starfarer.api.util.Misc;
import org.lwjgl.opengl.GL11;

import java.awt.Color;

/** Row of text in a list that highlights on hover, with the text getting cut off to fit. Expandable rows show an arrow. */
public class ListRowElement extends BaseElement {

    private static final float TEXT_OFFSET = 14f;
    private static final float ARROW_SIZE = 4f;
    private static final float ARROW_Y_OFFSET = -1f;
    private static final float HIGHLIGHT_ALPHA = 0.25f;

    private final FaderUtil glow = new FaderUtil(0f, 0.05f, 0.25f);
    private final LabelAPI label;
    private final LabelAPI countLabel;
    private final Color textColor;
    private final float countWidth;
    private final boolean expandable;
    private final boolean expanded;

    /** @param count Centered in the countWidth area at the end of the row, can be null. */
    public ListRowElement(UIPanelAPI parent, String text, String count, float countWidth, Color textColor, float width, float height, boolean expandable, boolean expanded) {
        super(parent, width, height);
        this.textColor = textColor;
        this.countWidth = countWidth;
        this.expandable = expandable;
        this.expanded = expanded;

        float textWidth = width - TEXT_OFFSET - countWidth;
        label = Global.getSettings().createLabel("", Fonts.DEFAULT_SMALL);
        label.getPosition().setSize(textWidth, height);
        label.setText(cutToWidth(text, textWidth));

        countLabel = count == null ? null : Global.getSettings().createLabel(count, Fonts.DEFAULT_SMALL);
        if (countLabel != null) {
            countLabel.setAlignment(Alignment.MID);
            countLabel.getPosition().setSize(countWidth, height);
        }

        onHoverEnter(event -> glow.fadeIn());
        onHoverExit(event -> glow.fadeOut());
    }

    private String cutToWidth(String text, float width) {
        float textWidth = label.computeTextWidth(text);
        if (textWidth <= width) return text;

        String cut = text.substring(0, Math.min(text.length(), (int) (text.length() * width / textWidth) + 1));
        while (!cut.isEmpty() && label.computeTextWidth(cut + "...") > width) {
            cut = cut.substring(0, cut.length() - 1);
        }
        return cut + "...";
    }

    @Override
    public void advance(float amount) {
        glow.advance(amount);
        super.advance(amount);
    }

    @Override
    public void renderBelow(float alphaMult) {
        float brightness = glow.getBrightness();

        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glEnable(GL11.GL_BLEND);

        if (brightness > 0f) {
            GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE);
            Misc.renderQuad(getPosition().getX(), getPosition().getY(), getPosition().getWidth(), getPosition().getHeight(),
                    Misc.getDarkPlayerColor(), alphaMult * brightness * HIGHLIGHT_ALPHA);
        }

        if (expandable) renderArrow(alphaMult);

        GL11.glEnable(GL11.GL_TEXTURE_2D);

        super.renderBelow(alphaMult);
    }

    private void renderArrow(float alphaMult) {
        float x = getPosition().getX() + TEXT_OFFSET / 2f;
        float y = getPosition().getCenterY() + ARROW_Y_OFFSET;

        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glColor4ub((byte) textColor.getRed(), (byte) textColor.getGreen(), (byte) textColor.getBlue(), (byte) (textColor.getAlpha() * alphaMult));
        GL11.glBegin(GL11.GL_TRIANGLES);
        if (expanded) {
            GL11.glVertex2f(x - ARROW_SIZE, y + ARROW_SIZE / 2f);
            GL11.glVertex2f(x + ARROW_SIZE, y + ARROW_SIZE / 2f);
            GL11.glVertex2f(x, y - ARROW_SIZE / 2f);
        } else {
            GL11.glVertex2f(x - ARROW_SIZE / 2f, y + ARROW_SIZE);
            GL11.glVertex2f(x - ARROW_SIZE / 2f, y - ARROW_SIZE);
            GL11.glVertex2f(x + ARROW_SIZE / 2f, y);
        }
        GL11.glEnd();
    }

    @Override
    public void render(float alphaMult) {
        Color color = Misc.interpolateColor(textColor, Color.white, glow.getBrightness());

        label.getPosition().setLocation(getPosition().getX() + TEXT_OFFSET, getPosition().getY());
        label.setColor(color);
        label.render(alphaMult);

        if (countLabel != null) {
            countLabel.getPosition().setLocation(getPosition().getX() + getPosition().getWidth() - countWidth, getPosition().getY());
            countLabel.setColor(color);
            countLabel.render(alphaMult);
        }

        super.render(alphaMult);
    }
}
