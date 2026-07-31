# PatchLib

A Starsector library mod that enables patching code at runtime.  
This can be used to modify the behaviour and values from vanilla methods.

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

This simple patch hooks after the execution of the games clock "getCycle" and always increases it by 1000 cycles.

## API Documentation

Full documentation can be found on the [wiki](https://github.com/Lukas22041/PatchLib/wiki).
