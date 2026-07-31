package patchlib.patches;

import com.fs.starfarer.api.combat.BaseHullMod;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;
import patchlib.api.context.AfterContext;
import patchlib.api.context.BeforeContext;
import patchlib.api.match.ClassMatch;
import patchlib.api.match.MethodMatch;
import patchlib.api.patch.After;
import patchlib.api.patch.Patch;

/*@Patch(target = @ClassMatch(subtype = BaseHullMod.class))
public class BaseHullmodTestPatch {

    @After(target = @MethodMatch(methodName = "addPostDescriptionSection"))
    public static void Test(AfterContext context) {
        TooltipMakerAPI tooltip = (TooltipMakerAPI) context.getArg(0);
        tooltip.addPara("\nTest", 0f, Misc.getHighlightColor(), Misc.getHighlightColor());
    }

}*/
