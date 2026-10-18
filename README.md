# PatchLib

A Starsector library mod that enables patching code at runtime.  
This can be used to modify the behavior and values of vanilla and modded methods.

[Latest Download](https://github.com/Lukas22041/PatchLib/releases/latest/download/PatchLib.zip) | [Older Versions & Changelogs](https://github.com/Lukas22041/PatchLib/releases)

## Installation

The mod is installed like any other mod, by dropping it into your /mods/ folder.

> **INCOMPATIBILITY**: PatchLib is currently incompatible with community-made upgrades of the game's **Java** version to Java 25 or higher. However, if you have the [Fast Rendering](https://fractalsoftworks.com/forum/index.php?topic=33870.0) mod installed, it will work fine. This is because Fast Rendering fixes the issue that causes this incompatibility in the first place. 

## AI-generated content disclaimer
The library has been co-developed with the help of Claude Code and the models Claude Fable 5, Claude Fable 5.1 and Claude Opus 5.5. 
The generated code has been heavily tested by real people and is understood by the maintainer(s). 
No visual assets were created with generative AI.

All documentation is written by hand, but has had grammatical corrections applied through AI tools.

# For developers

PatchLib provides a relatively simple annotation API for patching the game's code.  
If you are not used to such patterns in Java, it may feel a bit alien, but the concept is simple once you get used to it.  

The following is an example patch that modifies the return value of the campaign clock's ``getCycle`` method, changing it to always return the original value + 1000.

```java
@Patch(target = @ClassMatch(subtype = CampaignClockAPI.class))
public class TestPatch {

    @After(target = @MethodMatch(methodName = "getCycle"))
    public static void afterGetCycle(AfterContext context) {
        context.setReturnValue((int) context.getReturnValue() + 1000);
    }
}
```

## Documentation

Full documentation for mod developers can be found on the [wiki](https://github.com/Lukas22041/PatchLib/wiki).  
The documentation assumes that you have basic Java programming knowledge.  
Please mention it on Discord or in the forum post if you find issues or mistakes, or if anything is unclear.

## Contribution

The goal of the library is to stay relatively focused on its specific feature set and to keep the underlying code easy to maintain.
I would recommend approaching the mod's main maintainer (Lukas04) before working on large contributions, as, for the reasons above, they may be denied by default. 

### AI-generated code

AI-generated code is permitted for contributions, however:

- Vibe-coded contributions are not permitted and will not be merged. There needs to be a significant amount of human involvement and testing.
- Overly massive branches, or branches that do not follow repo conventions, will not be merged.
- Merge requests from largely autonomous, AI-agent-steered accounts will not be merged.
