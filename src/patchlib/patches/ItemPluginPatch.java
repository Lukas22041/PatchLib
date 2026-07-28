package patchlib.patches;

import com.fs.starfarer.api.campaign.impl.items.BaseSpecialItemPlugin;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;
import patchlib.api.context.AfterContext;
import patchlib.api.match.ClassMatch;
import patchlib.api.match.MethodMatch;
import patchlib.api.patch.After;
import patchlib.api.patch.Patch;

/*
@Patch(target = @ClassMatch(subtype = BaseSpecialItemPlugin.class))
public class ItemPluginPatch {

    @After(target = @MethodMatch(methodName = "createTooltip", parameterCount = 4))
    public static void handler(AfterContext context) {
        TooltipMakerAPI tooltip = (TooltipMakerAPI) context.getArg(0);

        if (context.isMostDerivedCall()) {
            tooltip.addPara("[PatchLib]", 0f, Misc.getHighlightColor(), Misc.getHighlightColor());
        }
    }

}
*/
