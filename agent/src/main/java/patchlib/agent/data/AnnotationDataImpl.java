package patchlib.agent.data;

import net.bytebuddy.description.annotation.AnnotationDescription;
import net.bytebuddy.description.annotation.AnnotationValue;
import net.bytebuddy.description.enumeration.EnumerationDescription;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.type.TypeDescription;
import patchlib.api.data.AnnotationData;

public class AnnotationDataImpl implements AnnotationData {

    private AnnotationDescription annotationDescription;

    public AnnotationDataImpl(AnnotationDescription annotationDescription) {
        this.annotationDescription = annotationDescription;
    }

    @Override
    public String getName() {
        return annotationDescription.getAnnotationType().asErasure().getActualName();
    }

    @Override
    public String getClassName(String id) {
        AnnotationValue<?, ?> value = get(id);
        return value != null ? value.resolve(TypeDescription.class).getActualName() : null;
    }

    @Override
    public String[] getClassNameArray(String id) {
        AnnotationValue<?, ?> value = get(id);
        if (value == null) return null;

        TypeDescription[] classes = value.resolve(TypeDescription[].class);
        String[] names = new String[classes.length];
        for (int i = 0; i < names.length; i++) {
            names[i] = classes[i].asErasure().getActualName();
        }

        return names;
    }

    @Override
    public String getString(String id) {
        AnnotationValue<?, ?> value = get(id);
        return value != null ? value.resolve(String.class) : null;
    }

    @Override
    public String[] getStringArray(String id) {
        AnnotationValue<?, ?> value = get(id);
        return value != null ? value.resolve(String[].class) : null;
    }

    @Override
    public Integer getInt(String id) {
        AnnotationValue<?, ?> value = get(id);
        return value != null ? value.resolve(Integer.class) : null;
    }

    @Override
    public Integer[] getIntArray(String id) {
        AnnotationValue<?, ?> value = get(id);
        return value != null ? value.resolve(Integer[].class) : null;
    }

    @Override
    public Float getFloat(String id) {
        AnnotationValue<?, ?> value = get(id);
        return value != null ? value.resolve(Float.class) : null;
    }

    @Override
    public Float[] getFloatArray(String id) {
        AnnotationValue<?, ?> value = get(id);
        return value != null ? value.resolve(Float[].class) : null;
    }

    @Override
    public Boolean getBoolean(String id) {
        AnnotationValue<?, ?> value = get(id);
        return value != null ? value.resolve(Boolean.class) : null;
    }

    @Override
    public Boolean[] getBooleanArray(String id) {
        AnnotationValue<?, ?> value = get(id);
        return value != null ? value.resolve(Boolean[].class) : null;
    }

    @Override
    public String getEnumValue(String id) {
        AnnotationValue<?, ?> value = get(id);
        return value != null ? value.resolve(EnumerationDescription.class).getValue() : null;
    }

    @Override
    public AnnotationData getAnnotation(String id) {
        AnnotationValue<?, ?> value = get(id);
        return value != null ? new AnnotationDataImpl(value.resolve(AnnotationDescription.class)) : null;
    }

    @Override
    public AnnotationData[] getAnnotationArray(String id) {
        AnnotationValue<?, ?> value = get(id);
        if (value == null) return null;

        AnnotationDescription[] descriptions = value.resolve(AnnotationDescription[].class);
        AnnotationData[] data = new AnnotationData[descriptions.length];

        for (int i = 0; i < descriptions.length; i++) {
            data[i] = new AnnotationDataImpl(descriptions[i]);
        }

        return data;
    }

    private AnnotationValue<?, ?> get(String id) {
        for (MethodDescription.InDefinedShape method : annotationDescription.getAnnotationType().getDeclaredMethods()) {
            if (method.getActualName().equals(id)) {
                return annotationDescription.getValue(method);
            }
        }
        return null;
    }
}
