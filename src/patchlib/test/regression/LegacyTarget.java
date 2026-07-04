package patchlib.test.regression;

/** Implemented by the janino compiled legacy target, so the test can call it without reflection.
 * Mod code runs on the script class loader, which blocks java.lang.reflect. */
public interface LegacyTarget {

    String beforeTarget();

    int afterTarget();

}
