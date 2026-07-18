package patchlib.api.scan;

import java.util.ArrayList;
import java.util.List;

public class MethodScanBuilder {

    private String methodName = "";
    private List<String> parameterNames = new ArrayList<>();

    private MethodScanBuilder() {

    }

    public static MethodScanBuilder create() {
        return new MethodScanBuilder();
    }

}
