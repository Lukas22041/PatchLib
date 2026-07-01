package patchlib.api.context;

/** Context for a @Redirect that intercepts a field read inside the host method. The inherited Context methods refer
 * to the host method; the methods below refer to the read. */
public interface FieldReadContext extends Context {

    /** The instance whose field is read. Null for a static field. */
    Object getFieldOwner();

    /** The instance whose field is read, cast to the type you assign it to. Null for a static field. */
    <T> T getInferredFieldOwner();

    /** Reads the field at the next layer down, or the original field if this is the innermost layer.
     * Returns the read value. This does not by itself become this layer's result, use setResult for that. */
    Object read();

    /** Sets the value this read yields to the host method. Must be set. */
    void setResult(Object result);
}
