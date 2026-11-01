package patchlib.ui.elements;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.graphics.SpriteAPI;
import com.fs.starfarer.api.ui.UIPanelAPI;
import com.fs.starfarer.api.util.FaderUtil;

import java.awt.Color;

public class CheckboxElement extends BaseElement {

    public static final float SIZE = 24f;

    private static final Color LOCKED_COLOR = new Color(110, 110, 110);

    private final SpriteAPI onSprite = Global.getSettings().getSprite("ui", "toggle20_on");
    private final SpriteAPI offSprite = Global.getSettings().getSprite("ui", "toggle20_off");
    private final FaderUtil glow = new FaderUtil(0f, 0.05f, 0.25f);
    private final boolean locked;
    private boolean checked;

    public CheckboxElement(UIPanelAPI parent, boolean checked, boolean locked) {
        super(parent, SIZE, SIZE);
        this.checked = checked;
        this.locked = locked;

        onHoverEnter(event -> {
            if (locked) return;
            Global.getSoundPlayer().playUISound("ui_button_mouseover", 1f, 1f);
            glow.fadeIn();
        });
        onHoverExit(event -> glow.fadeOut());
        onClick(event -> {
            if (!event.isLMBEvent()) return;
            if (locked) {
                Global.getSoundPlayer().playUISound("ui_button_disabled_pressed", 1f, 1f);
                return;
            }
            this.checked = !this.checked;
            Global.getSoundPlayer().playUISound("ui_button_pressed", 1f, 1f);
        });
    }

    public boolean isChecked() {
        return checked;
    }

    @Override
    public void advance(float amount) {
        glow.advance(amount);
        super.advance(amount);
    }

    @Override
    public void renderBelow(float alphaMult) {
        SpriteAPI sprite = checked ? onSprite : offSprite;
        float x = getPosition().getCenterX();
        float y = getPosition().getCenterY();

        sprite.setColor(locked ? LOCKED_COLOR : Color.white);
        sprite.setNormalBlend();
        sprite.setAlphaMult(alphaMult);
        sprite.renderAtCenter(x, y);

        if (glow.getBrightness() > 0f) {
            sprite.setAdditiveBlend();
            sprite.setAlphaMult(alphaMult * glow.getBrightness() * 0.25f);
            sprite.renderAtCenter(x, y);
        }

        super.renderBelow(alphaMult);
    }
}
