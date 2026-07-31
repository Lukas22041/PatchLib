package patchlib.agent.patch.redirect;

import patchlib.agent.context.ConstructorCallContextImpl;
import patchlib.agent.context.FieldReadContextImpl;
import patchlib.agent.context.FieldWriteContextImpl;
import patchlib.agent.context.MethodCallContextImpl;
import patchlib.agent.log.PatchLibLogger;
import patchlib.agent.patch.InstallationData;
import patchlib.agent.spec.PatchHandlerSpec;
import patchlib.api.context.ConstructorCallContext;
import patchlib.api.context.Context;

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

    private static final MethodHandle METHOD_CALL_CHAIN_RUN = createRunLayerHandle("runMethodCallLayer", CALL_CHAIN_TYPE);
    private static final MethodHandle CONSTRUCTOR_CALL_CHAIN_RUN = createRunLayerHandle("runConstructorCallLayer", CONSTRUCTOR_CHAIN_TYPE);
    private static final MethodHandle FIELD_READ_CHAIN_RUN = createRunLayerHandle("runFieldReadLayer", FIELD_READ_CHAIN_TYPE);
    private static final MethodHandle FIELD_WRITE_CHAIN_RUN = createRunLayerHandle("runFieldWriteLayer", FIELD_WRITE_CHAIN_TYPE);

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
            case METHOD_CALL -> METHOD_CALL_CHAIN_RUN;
            case CONSTRUCTOR -> CONSTRUCTOR_CALL_CHAIN_RUN;
            case FIELD_READ -> FIELD_READ_CHAIN_RUN;
            case FIELD_WRITE -> FIELD_WRITE_CHAIN_RUN;
        };
    }


    private static Object runMethodCallLayer(MethodHandle handler, String errorMessage, MethodHandle chain, Class<?> hostClass,
                                             Object callReceiver, Object[] callArgs, Object hostSelf, Object[] hostArgs) {
        MethodCallContextImpl context = new MethodCallContextImpl(hostClass, hostSelf, hostArgs, callReceiver, callArgs, chain);
        invoke(handler, context, errorMessage);
        return context.getResult();
    }

    private static Object runConstructorCallLayer(MethodHandle handler, String errorMessage, MethodHandle chain, Class<?> hostClass,
                                             Object[] constructorArgs, Object hostSelf, Object[] hostArgs) {
        ConstructorCallContextImpl context = new ConstructorCallContextImpl(hostClass, hostSelf, hostArgs, constructorArgs, chain);
        invoke(handler, context, errorMessage);
        return context.getResult();
    }

    private static Object runFieldReadLayer(MethodHandle handler, String errorMessage, MethodHandle chain, Class<?> hostClass,
                                             Object owner, Object hostSelf, Object[] hostArgs) {
        FieldReadContextImpl context = new FieldReadContextImpl(hostClass, hostSelf, hostArgs, owner, chain);
        invoke(handler, context, errorMessage);
        return context.getResult();
    }

    private static void runFieldWriteLayer(MethodHandle handler, String errorMessage, MethodHandle chain, Class<?> hostClass,
                                             Object owner, Object writtenValue, Object hostSelf, Object[] hostArgs) {
        FieldWriteContextImpl context = new FieldWriteContextImpl(hostClass, hostSelf, hostArgs, owner, writtenValue, chain);
        invoke(handler, context, errorMessage);
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

    private static void invoke(MethodHandle handler, Context context, String error) {
        try {
            handler.invokeExact(context);
        } catch (Throwable ex) {
            PatchLibLogger.error(error);
            throw uncheckedThrow(ex);
        }
    }

    @SuppressWarnings("unchecked")
    private static <T extends Throwable> T uncheckedThrow(Throwable ex) throws T {
        throw (T) ex;
    }

}
