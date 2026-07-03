package patchlib.agent.discover;

import net.bytebuddy.description.annotation.AnnotationDescription;
import net.bytebuddy.description.annotation.AnnotationValue;
import net.bytebuddy.description.enumeration.EnumerationDescription;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.type.TypeDescription;

/** Reads single members off a scanned annotation, with a fallback for members the annotation does not declare. */
final class AnnotationReader {

    private AnnotationReader() {}

    static String readString(AnnotationDescription annotation, String member, String fallback) {
        AnnotationValue<?, ?> value = find(annotation, member);
        return value != null ? value.resolve(String.class) : fallback;
    }

    static String[] readStringArray(AnnotationDescription annotation, String member) {
        AnnotationValue<?, ?> value = find(annotation, member);
        return value != null ? value.resolve(String[].class) : new String[0];
    }

    static boolean readBoolean(AnnotationDescription annotation, String member, boolean fallback) {
        AnnotationValue<?, ?> value = find(annotation, member);
        return value != null ? value.resolve(Boolean.class) : fallback;
    }

    static int readInt(AnnotationDescription annotation, String member, int fallback) {
        AnnotationValue<?, ?> value = find(annotation, member);
        //Has to be Integer.class, despite reading int.class, since bytebuddy boxes it.
        return value != null ? value.resolve(Integer.class) : fallback;
    }

    static String readEnumName(AnnotationDescription annotation, String member, String fallback) {
        AnnotationValue<?, ?> value = find(annotation, member);
        return value != null ? value.resolve(EnumerationDescription.class).getValue() : fallback;
    }

    static String readType(AnnotationDescription annotation, String member, String fallback) {
        AnnotationValue<?, ?> value = find(annotation, member);
        if (value == null) return fallback;
        String name = value.resolve(TypeDescription.class).getActualName();
        return name.equals(PatchScanner.UNSET) ? fallback : name;
    }

    static String[] readTypeArray(AnnotationDescription annotation, String member) {
        AnnotationValue<?, ?> value = find(annotation, member);
        if (value == null) return new String[0];

        Object[] raw = value.resolve(Object[].class); //elements are TypeDescription
        String[] names = new String[raw.length];
        for (int i = 0; i < raw.length; i++)
            names[i] = ((TypeDescription) raw[i]).getActualName();
        return names;
    }

    static AnnotationDescription readAnnotation(AnnotationDescription annotation, String member) {
        AnnotationValue<?, ?> value = find(annotation, member);
        return value != null ? (AnnotationDescription) value.resolve() : null;
    }

    static AnnotationDescription[] readAnnotationArray(AnnotationDescription annotation, String member) {
        AnnotationValue<?, ?> value = find(annotation, member);
        if (value == null) return new AnnotationDescription[0];

        Object[] raw = value.resolve(Object[].class); //elements are AnnotationDescription
        AnnotationDescription[] out = new AnnotationDescription[raw.length];
        for (int i = 0; i < raw.length; i++) out[i] = (AnnotationDescription) raw[i];
        return out;
    }

    /** The raw value of the named member, or null if the annotation does not declare it. */
    private static AnnotationValue<?, ?> find(AnnotationDescription annotation, String member) {
        for (MethodDescription.InDefinedShape m : annotation.getAnnotationType().getDeclaredMethods()) {
            if (m.getName().equals(member)) return annotation.getValue(m);
        }
        return null;
    }
}
