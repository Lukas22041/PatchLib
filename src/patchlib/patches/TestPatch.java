package patchlib.patches;

import com.fs.starfarer.api.Global;
import patchlib.api.context.MethodCallContext;
import patchlib.api.match.MethodCallMatch;
import patchlib.api.match.MethodMatch;
import patchlib.api.patch.Patch;
import patchlib.api.patch.Redirect;

/** Demonstrates @Redirect. Inside CampaignFleet.advance the fleet reads its own getTravelSpeed() and feeds the result
 * straight into its movement module, so redirecting that one call changes the player fleet's map speed without
 * touching how speed is computed or how any other fleet moves. Doing this with @Before/@After would mean reproducing
 * the whole advance method.
 *
 * Two layers target the same call to show how redirects nest. The lower priority is the outer layer and runs first;
 * its call() reaches the inner layer, whose call() reaches the real getTravelSpeed. The result flows back out, so for
 * the player the speed ends up (real + 50) * 2. Other fleets are left untouched. */
@Patch(targetClassName = "com.fs.starfarer.campaign.fleet.CampaignFleet")
public class TestPatch {

    private static long lastLog = 0L;

    /** Outer layer, runs first. Doubles whatever the inner layer hands back, for the player fleet only. */
    @Redirect(
            target = @MethodMatch(methodName = "advance", parameterNames = {"float"}),
            methodCall = @MethodCallMatch(methodName = "getTravelSpeed"),
            priority = 0
    )
    public static void outerDoubleSpeed(MethodCallContext context) {
        float fromInner = (Float) context.call(); //Reaches the inner layer, then the real getTravelSpeed
        float result = fromInner;
        if (isPlayerFleet(context)) {
            result = fromInner * 20f;
            if (logDue(true)) log("outer", "inner gave " + fromInner + ", returning " + result);
        }
        context.setResult(result);
    }

    /** Inner layer, reached through the outer layer. Adds a flat bonus on top of the real speed. */
    @Redirect(
            target = @MethodMatch(methodName = "advance", parameterNames = {"float"}),
            methodCall = @MethodCallMatch(methodName = "getTravelSpeed"),
            priority = 10
    )
    public static void innerFlatBonus(MethodCallContext context) {
        float real = (Float) context.call(); //Reaches the real getTravelSpeed
        float result = isPlayerFleet(context) ? real + 5f : real;
        if (isPlayerFleet(context) && logDue(false)) log("inner", "real " + real + ", returning " + result);
        context.setResult(result);

        //To skip the original entirely, never call context.call() and just set a value: context.setResult(500f);
    }

    /** The redirect runs for every fleet, so only act on the player. getSelf is the host instance, the fleet being advanced. */
    private static boolean isPlayerFleet(MethodCallContext context) {
        return context.getSelf() == Global.getSector().getPlayerFleet();
    }

    /** advance runs every frame, so only log occasionally. The inner layer peeks, the outer layer commits the window. */
    private static boolean logDue(boolean commit) {
        long now = System.currentTimeMillis();
        if (now - lastLog < 3000L) return false;
        if (commit) lastLog = now;
        return true;
    }

    private static void log(String layer, String message) {
        Global.getLogger(TestPatch.class).info("[PatchLib @Redirect " + layer + "] " + message);
    }
}
