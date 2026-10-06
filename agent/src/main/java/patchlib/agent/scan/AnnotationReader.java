package patchlib.agent.scan;

import net.bytebuddy.description.annotation.AnnotationDescription;
import net.bytebuddy.description.annotation.AnnotationValue;
import net.bytebuddy.description.enumeration.EnumerationDescription;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.type.TypeDescription;

/** Reads the values of an annotation found during discovery, mainly used to read the patch annotations. */
public class AnnotationReader {

    private AnnotationDescription annotationDescription;

    public AnnotationReader(AnnotationDescription annotationDescription) {
        this.annotationDescription = annotationDescription;
    }

    public String getName() {
        return annotationDescription.getAnnotationType().asErasure().getActualName();
    }

    public String getClassName(String id) {
        AnnotationValue<?, ?> value = get(id);
        return value != null ? value.resolve(TypeDescription.class).getActualName() : null;
    }

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

    public String getString(String id) {
        AnnotationValue<?, ?> value = get(id);
        return value != null ? value.resolve(String.class) : null;
    }

    public String[] getStringArray(String id) {
        AnnotationValue<?, ?> value = get(id);
        return value != null ? value.resolve(String[].class) : null;
    }

    public Integer getInt(String id) {
        AnnotationValue<?, ?> value = get(id);
        return value != null ? value.resolve(Integer.class) : null;
    }

    public int[] getIntArray(String id) {
        AnnotationValue<?, ?> value = get(id);
        return value != null ? value.resolve(int[].class) : null;
    }

    public Float getFloat(String id) {
        AnnotationValue<?, ?> value = get(id);
        return value != null ? value.resolve(Float.class) : null;
    }

    public float[] getFloatArray(String id) {
        AnnotationValue<?, ?> value = get(id);
        return value != null ? value.resolve(float[].class) : null;
    }

    public Boolean getBoolean(String id) {
        AnnotationValue<?, ?> value = get(id);
        return value != null ? value.resolve(Boolean.class) : null;
    }

    public boolean[] getBooleanArray(String id) {
        AnnotationValue<?, ?> value = get(id);
        return value != null ? value.resolve(boolean[].class) : null;
    }

    public String getEnumValue(String id) {
        AnnotationValue<?, ?> value = get(id);
        return value != null ? value.resolve(EnumerationDescription.class).getValue() : null;
    }

    public AnnotationReader getAnnotation(String id) {
        AnnotationValue<?, ?> value = get(id);
        return value != null ? new AnnotationReader(value.resolve(AnnotationDescription.class)) : null;
    }

    public AnnotationReader[] getAnnotationArray(String id) {
        AnnotationValue<?, ?> value = get(id);
        if (value == null) return null;

        AnnotationDescription[] descriptions = value.resolve(AnnotationDescription[].class);
        AnnotationReader[] data = new AnnotationReader[descriptions.length];

        for (int i = 0; i < descriptions.length; i++) {
            data[i] = new AnnotationReader(descriptions[i]);
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
