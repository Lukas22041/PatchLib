# PatchLib

A Starsector mod that enables patching code at runtime.  
This can be used to modify the behaviour and values from vanilla methods.

# 1. Installation

The mod requires some special setup. However, it comes bundled with an installer that starts when you start the game with the mod enabled.
As such most people should be fine to just start the game and follow the instructions there and install it like any other mod otherwise.
There are some cases that need special treatment below though.

### 1.1 Manual Installation

The manuall install involves to steps. First you copy `PatchLibAgent.jar` from the mods `/jars` folder next to the games launcher, and then you add `-javaagent:PatchLibAgent.jar` to the launch arguments, right after `-noverify`. Where exactly depends on your platform:

- **Windows:** Copy the jar into `starsector-core`, then add the flag to `vmparams` (in the game root, next to the exe).
- **Linux:** Copy the jar into the game root, next to `starsector.sh`, then add the flag to `starsector.sh` as its own continuation line after `-noverify`.
- **Mac:** Copy the jar into `Starsector.app/Contents/Resources/Java`, then add the flag to `Contents/MacOS/starsector_mac.sh`.

> **Warning:** The jar and the flag go together. If you remove `PatchLibAgent.jar` but leave the `-javaagent` flag in your launcher, the game wont start at all, since the JVM cant find the agent it was told to load. When uninstalling, remove both.

### 1.2 Fast Rendering

Currently incompatible until the next Fast Rendering update.

### 1.3 IntelliJ Mod Template

If you are a mod author and your mod is set up with [Wisps IntelliJ template](https://github.com/wispborne/Starsector-IntelliJ-Template), then it requires some manual installation. If you are using the Template from [Galatia Academy](https://galatia-academy.dev/wiki/mod-setup) then you can ignore this section. 
The issue is that wisps template has the vmparams for booting the game included in its run configurations, so if you make use of those, they will be missing the -javagent flag.
So either:

- Add `-javaagent:PatchLibAgent.jar` to the VM options of your run config yourself, right after `-noverify`, or
- Download the two premade run configs below and drop them into your mods `.run` folder.

-> [Run Starsector (PatchLib)](https://github.com/Lukas22041/PatchLib/blob/master/readme/Run%20Starsector_PatchLib.run.xml)  
-> [Run Starsector w/o Launcher (PatchLib)](https://github.com/Lukas22041/PatchLib/blob/master/readme/Run_Starsector_w_o_Launcher_PatchLib.run.xml)

Either way the agent jar still needs to sit in `starsector-core`, the same as the manual install above, but the installer will have also done this already for you.

> If you do not do this step, the the run configurations will repeatedly start the installer, instead of actually starting the game.

## 2. Guide for Modmakers

Full documentation is on the [wiki](https://github.com/Lukas22041/PatchLib/wiki).

As a quick example, a patch is just a class marked with `@Patch` that points at a target, with static methods marked `@Before` or `@After` that hook into it. 
This allows you to modify starsector code in many ways:

- Run code before or after starsector methods
- Replace or modify the return value of a method
- Replace or modify the arguments that a method receives
- Skip a method and run your own code instead

Following is an example patch. This patch is run after every "getCycle" call on starsectors campaign clock, and changes the value to be 1000 higher than its actual value.

```java
@Patch(targetSubtype = CampaignClockAPI.class)
public class TestPatch {

    @After(methodName = "getCycle")
    public static void afterGetCycle(PatchContext context) {
        context.setReturnValue((int) context.getReturnValue() + 1000);
    }

}
```

`@Patch` picks the class to patch, here anything implementing `CampaignClockAPI`. `@After` runs after `getCycle` returns, and `PatchContext` lets you read and overwrite the return value.
PatchLib automatically scans for the @Patch annotation, so you can leave your patches in whichever folder you want.

To make use of PatchLib in your mod, you want to add "PatchLibAPI.jar" from the `/jars` as a code dependency.
Do not add any of the other jars as a dependency. More instruction can be found in the [wiki](https://github.com/Lukas22041/PatchLib/wiki).
