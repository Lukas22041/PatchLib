package patchlib.patches;

import com.fs.starfarer.api.combat.BaseHullMod;
import patchlib.api.context.AfterContext;
import patchlib.api.context.BeforeContext;
import patchlib.api.match.ClassMatch;
import patchlib.api.match.MethodMatch;
import patchlib.api.patch.After;
import patchlib.api.patch.Patch;

@Patch(target = @ClassMatch(subtype = BaseHullMod.class))
public class BaseHullmodTestPatch {

    @After(target = @MethodMatch(methodName = "addPostDescriptionSection"))
    public static void Test(AfterContext context) {

    }

}
