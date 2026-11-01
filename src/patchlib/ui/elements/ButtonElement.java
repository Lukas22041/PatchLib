package patchlib.ui.elements;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.ui.Alignment;
import com.fs.starfarer.api.ui.ButtonAPI;
import com.fs.starfarer.api.ui.CutStyle;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.ui.UIPanelAPI;

import java.awt.Color;

public class ButtonElement extends BaseElement {

    private final ButtonAPI button;
    private final TooltipMakerAPI element;

    private Runnable pressListener;

    public ButtonElement(UIPanelAPI parent, String text, float width, float height, CutStyle cutStyle) {
        super(parent, width, height);

        Color textColor = Global.getSettings().getColor("buttonText");
        Color fillColor = Global.getSettings().getColor("buttonBgDark");

        element = elementPanel.createUIElement(width, height, false);
        button = element.addButton(text, this, textColor, fillColor, Alignment.MID, cutStyle, width, height, 0f);
        button.getPosition().inTL(0f, 0f).setXAlignOffset(0f);
        elementPanel.addUIElement(element).inTL(0f, 0f);
    }

    public void onPressed(Runnable listener) {
        pressListener = listener;
    }

    public void setText(String text) {
        button.setText(text);
    }

    public void addTooltip(TooltipMakerAPI.TooltipCreator tooltip, TooltipMakerAPI.TooltipLocation location) {
        element.addTooltipTo(tooltip, button, location, false);
    }

    public void setHighlighted(boolean highlighted) {
        if (highlighted) button.highlight();
        else button.unhighlight();
    }

    @Override
    public void buttonPressed(Object buttonId) {
        if (pressListener != null) pressListener.run();
    }
}
