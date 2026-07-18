package patchlib.api;

import patchlib.agent.scan.ClassScanner;
import patchlib.agent.scan.PatchScanner;
import patchlib.api.data.ClassData;

import java.util.List;

/** Agent side implementation to avoid circular dependency between the API and Agent side.
 * Placed in the "api" package to enable calling of package-private methods in the API. */
public class PatchLibImpl extends PatchLib {

    private ClassScanner scanner;

    private PatchLibImpl(ClassScanner scanner) {
        this.scanner = scanner;
    }

    public static void init(ClassScanner scanner) {
        PatchLib lib = new PatchLibImpl(scanner);
        PatchLib.setInstance(lib);
    }

    @Override
    protected List<ClassData> scanImpl() {
        return scanner.scan();
    }

}
