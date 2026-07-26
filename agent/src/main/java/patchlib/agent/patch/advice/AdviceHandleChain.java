package patchlib.agent.patch.advice;

import patchlib.agent.context.HookContextImpl;
import patchlib.agent.log.PatchLibLogger;
import patchlib.agent.patch.InstallationData;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.List;

public class AdviceHandleChain {

    //The type of the method handle chain, no return type & context parameter
    static MethodType handleChainType = MethodType.methodType(void.class, HookContextImpl.class);

    private static final MethodHandle INVOKE_HANDLE;
    static {
        try {
            INVOKE_HANDLE = MethodHandles.lookup().findStatic(AdviceHandleChain.class, "invoke",
                    MethodType.methodType(void.class, MethodHandle.class, String.class, HookContextImpl.class));
        } catch (IllegalAccessException | NoSuchMethodException ex) {
            throw new RuntimeException("Failed to create invoke handle", ex);
        }
    }

    public static MethodHandle createHandleChain(List<InstallationData> installationDataList) {
        if (installationDataList.isEmpty()) return MethodHandles.empty(handleChainType);

        //Layer the handles from the last handle to run up to the first on top.
        MethodHandle chain = wrap(installationDataList.get(installationDataList.size()-1));
        for (int i = installationDataList.size() - 2; i >= 0; i--) {
            chain = MethodHandles.foldArguments(chain, wrap(installationDataList.get(i)));
        }
        return chain;
    }

    /** Create a handle towards "invoke" with the handler and error message pre-provided, so that only the context is needed to invoke it. */
    private static MethodHandle wrap(InstallationData data) {
        return MethodHandles.insertArguments(INVOKE_HANDLE, 0, data.handler(), data.errorMessage());
    }

    /**
     * One step in the invokation chain
     * The error message is pre-provided as a mod patching the games mod specs can cause recursion if it was called after installation time
     * */
    static void invoke(MethodHandle handler, String errorMessage, HookContextImpl context) {
        try {
            handler.invokeExact(context);
        } catch (Throwable e) {
            PatchLibLogger.error(errorMessage);
            throw new RuntimeException(e);
        }
    }

}
