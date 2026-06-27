package patch_lib.api;

import patch_lib.api.query.FieldQuery;
import patch_lib.api.query.MethodQuery;
import patch_lib.api.ref.MethodRef;
import patch_lib.api.ref.Ref;
import patch_lib.api.store.PatchData;

public interface ExceptContext {

    Object getSelf();
    <T> T getInferredSelf();

    Object[] getArgs();
    Object getArg(int index);

    Throwable getThrown();
    <T extends Throwable> T getInferredThrown();

    void replaceThrown(Throwable newThrown);

    void suppressException(Object returnValue);

    boolean isSuppressed();

    <T> Ref<T> getArgRef(int index);

    <T> Ref<T> getField(FieldQuery query);

    <T> Ref<T> getField(Object instance, FieldQuery query);

    MethodRef getMethod(MethodQuery query);

    MethodRef getMethod(Object instance, MethodQuery query);

    boolean hasMethod(MethodQuery query);

    boolean hasMethod(Object instance, MethodQuery query);

    boolean hasField(FieldQuery query);

    boolean hasField(Object instance, FieldQuery query);

    PatchData getData();
}
