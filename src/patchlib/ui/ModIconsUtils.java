package patchlib.ui;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.ModSpecAPI;

import java.util.HashMap;
import java.util.Map;

/** Finds and loads the icons of mods. Either the one from a mods LunaLib settings config, or the icon.png in its root folder that TriOS uses.
 * Mods with neither use the codex icon for mod content. */
public class ModIconsUtils {

    private static final String LUNA_CONFIG = "data/config/LunaSettingsConfig.json";

    private static final Map<String, String> icons = new HashMap<>();

    /** Returns a path that can be used with getSprite. */
    public static String getIcon(ModSpecAPI mod) {
        if (!icons.containsKey(mod.getId())) icons.put(mod.getId(), findIcon(mod));
        return icons.get(mod.getId());
    }

    private static String findIcon(ModSpecAPI mod) {
        String lunaIcon = getLunaIcon(mod.getId());
        if (lunaIcon != null && loadTexture(lunaIcon)) return lunaIcon;

        //All icons are named the same, so this needs the full path, as relative ones resolve to the first mod that has the file.
        String rootIcon = mod.getPath() + "/icon.png";
        return loadTexture(rootIcon) ? rootIcon : Global.getSettings().getSpriteName("codex", "manual_other");
    }

    private static String getLunaIcon(String modId) {
        try {
            return Global.getSettings().loadJSON(LUNA_CONFIG, modId).getJSONObject(modId).getString("iconPath");
        } catch (Exception ex) {
            return null;
        }
    }

    private static boolean loadTexture(String path) {
        try {
            Global.getSettings().loadTexture(path);
            return true;
        } catch (Exception ex) {
            return false;
        }
    }
}
