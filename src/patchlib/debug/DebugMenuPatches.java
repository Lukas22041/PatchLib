package patchlib.debug;

import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.ui.UIPanelAPI;
import org.lwjgl.input.Keyboard;
import patchlib.api.context.BeforeContext;
import patchlib.api.match.ClassMatch;
import patchlib.api.match.MethodMatch;
import patchlib.api.patch.Before;
import patchlib.api.patch.Patch;

import java.util.List;

/** Opens the debug menu on Ctrl+P from any game state. Only opens and manages the panel, nothing else.
 * The event list arg is a subtype of ArrayList<InputEventAPI>, so it casts to List directly. */
@Patch(target = @ClassMatch(subtypeName = "com.fs.starfarer.BaseGameState"))
public class DebugMenuPatches {

    /** Master switch for the debug menu. */
    public static final boolean ENABLED = true;

    @SuppressWarnings("unchecked")
    @Before(target = @MethodMatch(methodName = "processInput", parameterCount = 2))
    public static void onProcessInput(BeforeContext context) {
        if (!ENABLED) return;

        Object self = context.getSelf();
        if (self == null) return;

        // a state switch discards the old screen panel, drop the menu with it
        if (DebugMenuManager.isOpen() && DebugMenuManager.getState() != self) {
            DebugMenuManager.close();
        }

        Object arg = context.getArg(0);
        if (!(arg instanceof List)) return;

        boolean toggle = false;
        for (InputEventAPI event : (List<InputEventAPI>) arg) {
            if (event.isConsumed()) continue;
            if (event.isKeyDownEvent() && event.getEventValue() == Keyboard.KEY_P && event.isCtrlDown()) {
                event.consume();
                toggle = true;
            }
        }
        if (!toggle) return;

        if (!context.hasMethod("getScreenPanel")) return; // the launcher state has no screen panel

        Object panel = context.getMethod("getScreenPanel").call();
        if (panel instanceof UIPanelAPI) {
            DebugMenuManager.toggle(self, (UIPanelAPI) panel);
        }
    }
}
