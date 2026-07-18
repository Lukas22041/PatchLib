package patchlib.agent.scan;

import patchlib.api.data.ClassData;

import java.util.List;

public class ClassScanner {

    private static List<ClassData> data;

    public static void setData(List<ClassData> data) {
        ClassScanner.data = data;
    }

}
