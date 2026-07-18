package patchlib.api;

import patchlib.api.data.ClassData;
import patchlib.api.query.ClassQuery;

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

    protected abstract List<ClassData> scanImpl(ClassQuery query, boolean includeGameClasses, boolean includeModClasses);

    //API


    /**
     * Method that enables scanning for classes in the game.
     * It has a notable limitation as in that it can not locate janino loaded classes, and is limited in general
     * to classes that are included in the games or mods jars.
     * */
    public static List<ClassData> scan(ClassQuery query) {
        return getInstance().scanImpl(query, true, true);
    }

    public static List<ClassData> scan(ClassQuery query, boolean includeGameClasses, boolean includeModClasses) {
        return getInstance().scanImpl(query, includeGameClasses, includeModClasses);
    }
}
