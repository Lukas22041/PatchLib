package patchlib.debug;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.ui.CustomPanelAPI;
import com.fs.starfarer.api.ui.UIPanelAPI;
import patchlib.agent.PatchLibLogger;
import patchlib.debug.ui.BackdropPlugin;
import patchlib.debug.ui.DebugWindow;

/** Owns the open/close lifecycle of the debug menu. Attaches two children to the current state's screen
 * panel: a full-screen backdrop that dims and captures leftover input, and the window on top of it. */
public final class DebugMenuManager {

    private static Object state;
    private static UIPanelAPI parent;
    private static CustomPanelAPI backdrop;
    private static DebugWindow window;

    // last dragged position in screen (bottom-left) coords, survives close; < 0 means center on next open
    private static float windowX = -1f;
    private static float windowY = -1f;

    private DebugMenuManager() { }

    public static boolean isOpen() {
        return window != null;
    }

    public static Object getState() {
        return state;
    }

    public static void toggle(Object gameState, UIPanelAPI screen) {
        if (isOpen()) {
            close();
        } else {
            open(gameState, screen);
        }
    }

    public static void open(Object gameState, UIPanelAPI screen) {
        if (isOpen()) close();
        try {
            float screenW = Global.getSettings().getScreenWidth();
            float screenH = Global.getSettings().getScreenHeight();

            state = gameState;
            parent = screen;

            backdrop = Global.getSettings().createCustom(screenW, screenH, new BackdropPlugin());
            screen.addComponent(backdrop).inTL(0f, 0f);

            window = new DebugWindow();
            window.create(screen, windowX, windowY, screenW, screenH);

            screen.bringComponentToTop(backdrop);
            screen.bringComponentToTop(window.getPanel());
        } catch (Exception e) {
            PatchLibLogger.error("Failed to open debug menu: " + e);
            close();
        }
    }

    public static void close() {
        try {
            if (parent != null) {
                if (window != null && window.getPanel() != null) parent.removeComponent(window.getPanel());
                if (backdrop != null) parent.removeComponent(backdrop);
            }
        } catch (Exception e) {
            PatchLibLogger.error("Failed to close debug menu: " + e);
        } finally {
            state = null;
            parent = null;
            backdrop = null;
            window = null;
        }
    }

    /** Called by the window chrome as it moves, so a reopened window keeps its place. */
    public static void saveWindowPos(float x, float y) {
        windowX = x;
        windowY = y;
    }

    public static boolean isInsideWindow(float x, float y) {
        return window != null && window.contains(x, y);
    }
}
