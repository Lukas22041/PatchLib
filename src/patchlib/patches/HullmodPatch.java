package patchlib.patches;

import com.fs.starfarer.api.combat.BaseHullMod;
import patchlib.api.patch.After;
import patchlib.api.patch.Patch;

@Patch(targetSubtype = BaseHullMod.class)
public class HullmodPatch {

    @After
    public static void afterAddPostDescriptionSection() {

    }

}
