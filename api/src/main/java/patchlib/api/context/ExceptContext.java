package patchlib.api.context;


public interface ExceptContext extends AdviceContext {

    /** Gets the exception that was thrown on the patched method. Can be null if another patch supressed the exception, and can also be another patches replaced exception. */
    Throwable getThrown();

    /** Replaces the thrown exception with your own */
    void replaceThrown(Throwable newThrown);

    /** Suppresses an exception, requires passing a return value. Use "null" for void methods.*/
    void suppressException(Object returnValue);

    /** Checks if another patch already suppressed the exception */
    boolean isSuppressed();

    /** Checks if something has skipped the original method in @Before */
    boolean isSkipOriginal();

}
