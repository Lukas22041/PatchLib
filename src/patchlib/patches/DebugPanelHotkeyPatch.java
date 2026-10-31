package patchlib.patches;

import com.fs.starfarer.BaseGameState;
import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.ui.UIPanelAPI;
import org.lwjgl.input.Keyboard;
import patchlib.api.context.BeforeContext;
import patchlib.api.match.ClassMatch;
import patchlib.api.match.MethodMatch;
import patchlib.api.patch.Before;
import patchlib.api.patch.Patch;
import patchlib.api.query.MethodQuery;
import patchlib.api.spec.MethodQuerySpec;
import patchlib.ui.DebugPanel;

import java.util.List;

/** Patch on the game states to listen for PatchLib's CTRL + P debug hotkey, which then attaches the panel to the screen panel*/
@Patch(target = @ClassMatch(subtype = BaseGameState.class))
public class DebugPanelHotkeyPatch {

    private static final MethodQuerySpec GET_SCREEN_PANEL = MethodQuery.create().methodName("getScreenPanel").parameterCount(0).build();

    @SuppressWarnings("unchecked")
    @Before(target = @MethodMatch(methodName = "processInput", parameterCount = 2))
    public static void beforeProcessInput(BeforeContext context) {
        for (InputEventAPI event : (List<InputEventAPI>) context.getArg(0)) {
            if (event.isConsumed() || event.isRepeat() || !event.isKeyDownEvent()) continue;
            if (event.getEventValue() != Keyboard.KEY_P || !event.isCtrlDown()) continue;

            if (!context.hasMethod(GET_SCREEN_PANEL)) return;

            event.consume();
            UIPanelAPI screenPanel = context.getMethod(GET_SCREEN_PANEL).call();
            DebugPanel.toggle(screenPanel);
            return;
        }
    }
}
