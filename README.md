# PatchLib

A Starsector library mod that enables patching code at runtime.  
This can be used to modify the behavior and values from vanilla and modded methods.

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

This simple patch hooks after the execution of the games clock "getCycle" and modiofies its retujrn value to always increase it by 1000 cycles.

## API Documentation

Full documentation can be found on the [wiki](https://github.com/Lukas22041/PatchLib/wiki).

## Contribution

The goal of the library is stay relatively focused on its specific feature set and to keep the underlying code easy to maintain.
I would recommend to approach the mods main maintainer (Lukas04) before working on large contributions, as for the reasons above, they may be denied by default. 

### AI generated code

AI generated code is permitted for contributions, however:

- Vibe-coded contributions are not permitted and will not be merged. There needs to be a significant amount of human involvement and testing.
- Overly massive branches, or branches that do not follow repo conventions will not merged.
- Merge requests from largely autonomous, ai agent steered accounts will not be merged.