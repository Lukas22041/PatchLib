package patchlib.api.context;

import patchlib.api.ref.Ref;

/** Context for a @Redirect that intercepts a field write inside the host method. The inherited Context methods refer
 * to the host method; the methods below refer to the write. */
public interface FieldWriteContext extends Context {

    /** The instance whose field is written. Null for a static field. */
    Object getFieldOwner();

    /** The instance whose field is written, cast to the type you assign it to. Null for a static field. */
    <T> T getInferredFieldOwner();

    /** The value being written to the field. */
    Object getValue();

    /** The value being written, cast to the type you assign it to. */
    <T> T getInferredValue();

    /** Replaces the value being written to the field. */
    void setValue(Object value);

    /** A typed read/writeable reference to the value being written. */
    <T> Ref<T> getValueRef();

    /** Performs the write at the next layer down, or the original write if this is the innermost layer, using the
     * current value. Skip the write entirely by never calling this. Returns null, a write has no result. */
    Object call();
}
