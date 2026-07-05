package patchlib.debug.ui;

import com.fs.starfarer.api.campaign.BaseCustomUIPanelPlugin;
import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.ui.PositionAPI;
import org.lwjgl.input.Keyboard;
import patchlib.debug.DebugMenuManager;

import java.awt.Color;
import java.util.List;

/** Full-screen sibling below the window. Dims everything and, since it sits above the game but below the
 * window, consumes every event the window did not, so nothing reaches the game. Click outside the window
 * or press escape to close. */
public class BackdropPlugin extends BaseCustomUIPanelPlugin {

    private static final float DIM_ALPHA = 0.4f;

    private PositionAPI position;

    @Override
    public void positionChanged(PositionAPI position) {
        this.position = position;
    }

    @Override
    public void renderBelow(float alphaMult) {
        if (position == null) return;
        DebugRender.fillRect(position.getX(), position.getY(), position.getWidth(), position.getHeight(),
                Color.BLACK, DIM_ALPHA * alphaMult);
    }

    @Override
    public void processInput(List<InputEventAPI> events) {
        for (InputEventAPI event : events) {
            if (event.isConsumed()) continue;

            if (event.isKeyDownEvent() && event.getEventValue() == Keyboard.KEY_ESCAPE) {
                DebugMenuManager.close();
                event.consume();
                continue;
            }
            if (event.isMouseDownEvent() && !DebugMenuManager.isInsideWindow(event.getX(), event.getY())) {
                DebugMenuManager.close();
            }
            // swallow whatever the window did not handle so the game below never sees it
            event.consume();
        }
    }
}
