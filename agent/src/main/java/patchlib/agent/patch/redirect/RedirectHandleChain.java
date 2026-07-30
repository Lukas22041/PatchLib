package patchlib.agent.patch.redirect;

import patchlib.agent.context.HookContextImpl;
import patchlib.agent.patch.InstallationData;
import patchlib.agent.patch.advice.AdviceHandleChain;
import patchlib.agent.spec.PatchHandlerSpec;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class RedirectHandleChain {

    //Return type, then parameter types
    private static final MethodType CALL_CHAIN_TYPE = MethodType.methodType(Object.class, Object.class, Object[].class, Object.class, Object[].class);
    private static final MethodType CONSTRUCTOR_CHAIN_TYPE = MethodType.methodType(Object.class, Object[].class, Object.class, Object[].class);
    private static final MethodType FIELD_READ_CHAIN_TYPE = MethodType.methodType(Object.class, Object.class, Object.class, Object[].class);
    private static final MethodType FIELD_WRITE_CHAIN_TYPE = MethodType.methodType(void.class, Object.class, Object.class, Object.class, Object[].class);

    private static final MethodHandle CALL_CHAIN_RUN = createRunLayerHandle(, CALL_CHAIN_TYPE);

    static MethodHandle wrapLayers(RedirectPatchSite site, MethodHandle convertedOriginal, Class<?> hostClass) {
        MethodHandle chain = convertedOriginal;
        List<InstallationData> layers = new ArrayList<>(site.installationDataList());
        Collections.reverse(layers); //Invert the order, since later priorities are the lower layers

        MethodHandle runHandle = getLayerRunHandle(site.redirectType());

        for (InstallationData layer : layers) {
            //Create a method handle that receives a chain of every deeper layer so far and gets all other patch related parameters pre-configured, so that only
            //the actual runtime parameters need to be provided by the redirect delegate later. Notably each redirect types chain has different parameters,
            //but those first 4 are shared by all of them.
            chain = MethodHandles.insertArguments(runHandle, 0, layer.handler(), layer.errorMessage(), chain, hostClass);
        }
        return chain;
    }

    /** Converts the method handle to the original, replaced call to one that fits the exact same typing as the chain, so that it can be seemlessly called from another part of the chain */
    static MethodHandle convertOriginalToChainHandle(PatchHandlerSpec.RedirectType redirectType, MethodHandle original, boolean hasReceiver) {
        return switch (redirectType) {
            case METHOD_CALL ->
            case CONSTRUCTOR ->
            case FIELD_READ ->
            case FIELD_WRITE ->
        }
    }

    private static MethodHandle getLayerRunHandle(PatchHandlerSpec.RedirectType redirectType) {
        return switch (redirectType) {
            case METHOD_CALL ->
            case CONSTRUCTOR ->
            case FIELD_READ ->
            case FIELD_WRITE ->
        }
    }


    private static Object runCallLayer(MethodHandle handler, String errorMessage, MethodHandle chain, Class<?> hostClass,
                                       Object callReceiver, Object[] callArgs, Object hostSelf, Object[] hostArgs) {



    }

    private static MethodHandle createRunLayerHandle(String methodName, MethodType chainType) {
        try {
            //Insert the 4 parameter types that each redirect chain type share
            MethodType layerType = chainType.insertParameterTypes(0, MethodHandle.class, String.class, MethodHandle.class, Class.class);
            return MethodHandles.lookup().findStatic(RedirectHandleChain.class, methodName, layerType);
        } catch (NoSuchMethodException|IllegalAccessException ex) {
            throw new RuntimeException("Failed to create handle to redirect run method", ex);
        }
    }

}
