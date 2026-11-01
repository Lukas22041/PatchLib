package patchlib.ui.elements;

import com.fs.starfarer.api.ui.PositionAPI;
import com.fs.starfarer.api.ui.UIPanelAPI;
import com.fs.starfarer.api.util.Misc;
import org.lwjgl.opengl.GL11;

import java.awt.Color;

public class BoxElement extends BaseElement {

    public BoxElement(UIPanelAPI parent, float width, float height) {
        super(parent, width, height);
    }

    @Override
    public void renderBelow(float alphaMult) {
        PositionAPI position = getPosition();
        float x = position.getX();
        float y = position.getY();
        float width = position.getWidth();
        float height = position.getHeight();
        Color color = Misc.getDarkPlayerColor();

        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        Misc.renderQuad(x, y, width, 1f, color, alphaMult);
        Misc.renderQuad(x, y + height - 1f, width, 1f, color, alphaMult);
        Misc.renderQuad(x, y, 1f, height, color, alphaMult);
        Misc.renderQuad(x + width - 1f, y, 1f, height, color, alphaMult);
        GL11.glEnable(GL11.GL_TEXTURE_2D);

        super.renderBelow(alphaMult);
    }
}
