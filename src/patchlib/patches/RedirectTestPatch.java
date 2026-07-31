package patchlib.patches;

import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.FleetDataAPI;
import com.fs.starfarer.campaign.CampaignEngine;
import com.fs.starfarer.campaign.fleet.CampaignFleet;
import com.fs.starfarer.campaign.fleet.SmoothMovementModule;
import patchlib.api.context.MethodCallContext;
import patchlib.api.match.ClassMatch;
import patchlib.api.match.MethodMatch;
import patchlib.api.patch.Patch;
import patchlib.api.patch.RedirectMethodCall;

/*@Patch(target = @ClassMatch(type = CampaignFleet.class))
public class RedirectTestPatch {

    @RedirectMethodCall(target = @MethodMatch(methodName = "advance"), call = @MethodMatch(methodName = "getTravelSpeed"), owner = @ClassMatch(subtype = FleetDataAPI.class))
    public static void testMethod(MethodCallContext context) {
        CampaignFleetAPI fleet = context.getInferredSelf();

        float speed = (float) context.call();
        if (fleet.isPlayerFleet()) {
            context.setResult(speed * 2000f);
            return;
        }
        context.setResult(speed);
    }

}*/
