package patch_lib.patches;

import com.fs.starfarer.api.campaign.CampaignClockAPI;
import patch_lib.api.patch.After;
import patch_lib.api.patch.Patch;
import patch_lib.api.PatchContext;

@Patch(targetSubtype = CampaignClockAPI.class)
public class TestPatch {

    @After(methodName = "getCycle")
    public static void afterGetCycle(PatchContext context) {
        context.setReturnValue((int) context.getReturnValue() + 1000);
    }

}
