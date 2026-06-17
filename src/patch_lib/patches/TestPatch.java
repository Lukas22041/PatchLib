package patch_lib.patches;

import com.fs.starfarer.api.campaign.CampaignClockAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import patch_lib.api.After;
import patch_lib.api.MethodMatch;
import patch_lib.api.Patch;

@Patch(targetSubtype = CampaignClockAPI.class)
public class TestPatch {

    @After(methodName = "getCycle")
    public static void afterGetCycle() {

    }

}
