package patch_lib.patches;

import com.fs.starfarer.api.combat.ShipAPI;
import patch_lib.api.MethodMatch;
import patch_lib.api.Patch;

@Patch(targetSubtype = ShipAPI.class)
public class TestPatch {


    public static void testMethod() {

    }

}
