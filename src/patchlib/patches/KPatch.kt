package patchlib.patches

import com.fs.starfarer.api.combat.BaseHullMod
import com.fs.starfarer.api.ui.TooltipMakerAPI
import com.fs.starfarer.api.util.Misc
import patchlib.api.AfterContext
import patchlib.api.patch.After
import patchlib.api.patch.Patch
import patchlib.api.query.FieldQuery

@Patch(targetSubtype = BaseHullMod::class)
object KPatch {


    var query = FieldQuery.named("query")

    @JvmStatic
    @After(methodName = "addPostDescriptionSection", )
    fun afterPostDescriptionSection(context: AfterContext) {
        var tooltip = context.getArg(0) as TooltipMakerAPI
        tooltip.addPara("Description added by PatchLib!", 0f,
            Misc.getHighlightColor(), Misc.getHighlightColor())
    }



}