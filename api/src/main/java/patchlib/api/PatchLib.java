package patchlib.api;

import patchlib.api.data.ClassData;
import patchlib.api.query.ClassQuery;
import patchlib.api.spec.ClassQuerySpec;

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

    protected abstract List<ClassData> scanImpl(ClassQuerySpec querySpec, boolean includeGameClasses, boolean includeModClasses);

    //API


    /**
     * Method that enables scanning for classes in the game.
     * It has a notable limitation as in that it can not locate janino loaded classes, and is limited in general
     * to classes that are included in the games or mods jars.
     * */
    public static List<ClassData> scan(ClassQuerySpec querySpec) {
        return getInstance().scanImpl(querySpec, true, true);
    }

    /**
     * Method that enables scanning for classes in the game.
     * It has a notable limitation as in that it can not locate janino loaded classes, and is limited in general
     * to classes that are included in the games or mods jars.
     * */
    public static List<ClassData> scan(ClassQuerySpec querySpec, boolean includeGameClasses, boolean includeModClasses) {
        return getInstance().scanImpl(querySpec, includeGameClasses, includeModClasses);
    }
}
