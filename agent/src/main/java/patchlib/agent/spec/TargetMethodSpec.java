package patchlib.agent.spec;

import patchlib.api.match.MethodType;

/** Contains data for resolving a target method based on certain attributes */
public record TargetMethodSpec(
        String methodName,
        String[] parameters,
        int parameterCount,
        String returnType,
        MethodType methodType,
        boolean staticOnly
) {



}
