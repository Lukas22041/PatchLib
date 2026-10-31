package patchlib.ui.elements;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.BaseCustomUIPanelPlugin;
import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.ui.CustomPanelAPI;
import com.fs.starfarer.api.ui.PositionAPI;
import com.fs.starfarer.api.ui.UIPanelAPI;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class BaseElement extends BaseCustomUIPanelPlugin {

    protected final CustomPanelAPI elementPanel;

    private boolean hovering = false;
    private boolean held = false;

    private final List<Consumer<PositionAPI>> positionListeners = new ArrayList<>();
    private final List<Consumer<Float>> advanceListeners = new ArrayList<>();
    private final List<Consumer<Float>> renderListeners = new ArrayList<>();
    private final List<Consumer<Float>> renderBelowListeners = new ArrayList<>();
    private final List<Consumer<List<InputEventAPI>>> inputListeners = new ArrayList<>();

    private final List<Consumer<InputEventAPI>> clickListeners = new ArrayList<>();
    private final List<Consumer<InputEventAPI>> clickOutsideListeners = new ArrayList<>();
    private final List<Consumer<InputEventAPI>> heldListeners = new ArrayList<>();
    private final List<Consumer<InputEventAPI>> hoverListeners = new ArrayList<>();
    private final List<Consumer<InputEventAPI>> hoverEnterListeners = new ArrayList<>();
    private final List<Consumer<InputEventAPI>> hoverExitListeners = new ArrayList<>();

    public BaseElement(UIPanelAPI parent, float width, float height) {
        elementPanel = Global.getSettings().createCustom(width, height, this);
        parent.addComponent(elementPanel);
    }

    public CustomPanelAPI getElementPanel() {
        return elementPanel;
    }

    public PositionAPI getPosition() {
        return elementPanel.getPosition();
    }

    public boolean isHovering() {
        return hovering;
    }

    public boolean isHeld() {
        return held;
    }

    public void onPositionChanged(Consumer<PositionAPI> listener) {
        positionListeners.add(listener);
    }

    public void onAdvance(Consumer<Float> listener) {
        advanceListeners.add(listener);
    }

    public void onRender(Consumer<Float> listener) {
        renderListeners.add(listener);
    }

    public void onRenderBelow(Consumer<Float> listener) {
        renderBelowListeners.add(listener);
    }

    public void onInput(Consumer<List<InputEventAPI>> listener) {
        inputListeners.add(listener);
    }

    public void onClick(Consumer<InputEventAPI> listener) {
        clickListeners.add(listener);
    }

    public void onClickOutside(Consumer<InputEventAPI> listener) {
        clickOutsideListeners.add(listener);
    }

    public void onHeld(Consumer<InputEventAPI> listener) {
        heldListeners.add(listener);
    }

    public void onHover(Consumer<InputEventAPI> listener) {
        hoverListeners.add(listener);
    }

    public void onHoverEnter(Consumer<InputEventAPI> listener) {
        hoverEnterListeners.add(listener);
    }

    public void onHoverExit(Consumer<InputEventAPI> listener) {
        hoverExitListeners.add(listener);
    }

    private static <T> void run(List<Consumer<T>> listeners, T value) {
        for (Consumer<T> listener : listeners) {
            listener.accept(value);
        }
    }

    @Override
    public void positionChanged(PositionAPI position) {
        run(positionListeners, position);
    }

    @Override
    public void advance(float amount) {
        run(advanceListeners, amount);
    }

    @Override
    public void renderBelow(float alphaMult) {
        run(renderBelowListeners, alphaMult);
    }

    @Override
    public void render(float alphaMult) {
        run(renderListeners, alphaMult);
    }

    @Override
    public void processInput(List<InputEventAPI> events) {
        for (InputEventAPI event : events) {
            if (event.isConsumed() || !event.isMouseEvent()) continue;

            boolean inside = getPosition().containsEvent(event);

            if (event.isMouseMoveEvent()) {
                if (inside && !hovering) {
                    hovering = true;
                    run(hoverEnterListeners, event);
                } else if (!inside && hovering) {
                    hovering = false;
                    run(hoverExitListeners, event);
                }

                if (inside) run(hoverListeners, event);
            }

            if (event.isMouseDownEvent()) {
                if (inside) {
                    held = true;
                    run(clickListeners, event);
                    event.consume();
                } else {
                    run(clickOutsideListeners, event);
                }
            } else if (event.isMouseUpEvent()) {
                held = false;
            } else if (held) {
                run(heldListeners, event);
                event.consume();
            }
        }

        run(inputListeners, events);
    }
}
