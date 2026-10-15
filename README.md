# PatchLib

A Starsector library mod that enables patching code at runtime.  
This can be used to modify the behavior and values from vanilla and modded methods.

## Installation

The mod is installed like any other mod, by dropping it in to your /mods/ folder.

> **INCOMPATIBILITY**: PatchLib is currently incompatible with community made upgrades of the games **java** version to Java25 or higher. However, if you have the [Fast Rendering](https://fractalsoftworks.com/forum/index.php?topic=33870.0) mod installed, it will work fine. This is due to the fact that fast rendering fixes the issue that causes this incompatibility in the first place. 

## AI-Generated Content Disclaimer
The library has been developed with the use of Claude Code with the models Claude Fable 5, Claude Fable 5.1 and Claude Opus 5.5.  
The generated code has been heavily tested by real people and the code is understood by the maintainer(s).  
No visual assets were created with generative AI.

# For Developers

A typical patch is registered through an annotation API like this:

```java
@Patch(target = @ClassMatch(subtype = CampaignClockAPI.class))
public class TestPatch {

    @After(target = @MethodMatch(methodName = "getCycle"))
    public static void afterGetCycle(AfterContext context) {
        context.setReturnValue((int) context.getReturnValue() + 1000);
    }
}
```

This simple patch hooks after the execution of the games clock "getCycle" and modifies its return value to always increase it by 1000 cycles.

## API Documentation

Full documentation for mod developers can be found on the [wiki](https://github.com/Lukas22041/PatchLib/wiki).  
Please mention on discord or the forum post if you find issues/mistakes within it, or if things are unclear.

## Contribution

The goal of the library is stay relatively focused on its specific feature set and to keep the underlying code easy to maintain.
I would recommend to approach the mods main maintainer (Lukas04) before working on large contributions, as for the reasons above, they may be denied by default. 

### AI generated code

AI generated code is permitted for contributions, however:

- Vibe-coded contributions are not permitted and will not be merged. There needs to be a significant amount of human involvement and testing.
- Overly massive branches, or branches that do not follow repo conventions will not merged.
- Merge requests from largely autonomous, ai agent steered accounts will not be merged.