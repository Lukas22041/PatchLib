package patchlib.api;

import patchlib.api.data.ClassData;

import java.util.List;

public abstract class PatchLib {

    //Internal Implementation
    private static PatchLib instance;

    static void setInstance(PatchLib instance) {
        PatchLib.instance = instance;
    }

    private static PatchLib getInstance() {
        if (instance == null) {
            throw new RuntimeException("PatchLib has not initialised yet. You can not call it's methods at this point.");
        }
        return instance;
    }

    protected abstract List<ClassData> scanImpl();

    //API
    public static List<ClassData> scan() {
        return getInstance().scanImpl();
    }
}
