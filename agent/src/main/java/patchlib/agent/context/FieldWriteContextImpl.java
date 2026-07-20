package patchlib.agent.context;

import patchlib.api.context.FieldReadContext;
import patchlib.api.context.FieldWriteContext;
import patchlib.api.ref.Ref;

public class FieldWriteContextImpl extends BaseContextImpl implements FieldWriteContext {

    /**
     * The instance whose field is written. Null for a static field.
     */
    @Override
    public Object getFieldOwner() {
        return null;
    }

    /**
     * The instance whose field is written, cast to the type you assign it to. Null for a static field.
     */
    @Override
    public <T> T getInferredFieldOwner() {
        return null;
    }

    /**
     * The value being written to the field.
     */
    @Override
    public Object getValue() {
        return null;
    }

    /**
     * The value being written, cast to the type you assign it to.
     */
    @Override
    public <T> T getInferredValue() {
        return null;
    }

    /**
     * Replaces the value being written to the field.
     *
     * @param value
     */
    @Override
    public void setValue(Object value) {

    }

    /**
     * A typed read/writeable reference to the value being written.
     */
    @Override
    public <T> Ref<T> getValueRef() {
        return null;
    }

    /**
     * Performs the write at the next layer down, or the original write if this is the innermost layer, using the
     * current value. Skip the write entirely by never calling this.
     */
    @Override
    public void write() {

    }

    /**
     * Same as write() but stores the given value instead of the current one.
     *
     * @param value
     */
    @Override
    public void write(Object value) {

    }
}
