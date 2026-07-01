package patchlib.agent.discover;

import net.bytebuddy.description.annotation.AnnotationDescription;
import net.bytebuddy.description.enumeration.EnumerationDescription;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.type.TypeDescription;

public class AnnotationReader {

    static String readString(AnnotationDescription annotation, String member, String fallback) {
        for (MethodDescription.InDefinedShape m : annotation.getAnnotationType().getDeclaredMethods()) {
            if (m.getName().equals(member)) return annotation.getValue(m).resolve(String.class);
        }
        return fallback;
    }

    static String[] readStringArray(AnnotationDescription annotation, String member) {
        for (MethodDescription.InDefinedShape m : annotation.getAnnotationType().getDeclaredMethods()) {
            if (m.getName().equals(member)) return annotation.getValue(m).resolve(String[].class);
        }
        return new String[0];
    }

    static boolean readBoolean(AnnotationDescription annotation, String member, boolean fallback) {
        for (MethodDescription.InDefinedShape m : annotation.getAnnotationType().getDeclaredMethods()) {
            if (m.getName().equals(member)) return annotation.getValue(m).resolve(Boolean.class);
        }
        return fallback;
    }

    static int readInt(AnnotationDescription annotation, String member, int fallback) {
        for (MethodDescription.InDefinedShape m : annotation.getAnnotationType().getDeclaredMethods()) {
            if (m.getName().equals(member)) return annotation.getValue(m).resolve(Integer.class); //Has to be Integer.class, despite reading int.class, since bytebuddy boxes it.
        }
        return fallback;
    }

    static String readEnumName(AnnotationDescription annotation, String member, String fallback) {
        for (MethodDescription.InDefinedShape m : annotation.getAnnotationType().getDeclaredMethods()) {
            if (m.getName().equals(member))
                return annotation.getValue(m).resolve(EnumerationDescription.class).getValue();
        }
        return fallback;
    }

    static String readType(AnnotationDescription annotation, String member, String fallback) {
        for (MethodDescription.InDefinedShape m : annotation.getAnnotationType().getDeclaredMethods()) {
            if (m.getName().equals(member)) {
                String name = annotation.getValue(m).resolve(TypeDescription.class).getActualName();
                return name.equals(PatchScanner.UNSET) ? fallback : name;
            }
        }
        return fallback;
    }

    static String[] readTypeArray(AnnotationDescription annotation, String member) {
        for (MethodDescription.InDefinedShape m : annotation.getAnnotationType().getDeclaredMethods()) {
            if (m.getName().equals(member)) {
                Object[] raw = annotation.getValue(m).resolve(Object[].class);   // elements are TypeDescription
                String[] names = new String[raw.length];
                for (int i = 0; i < raw.length; i++)
                    names[i] = ((TypeDescription) raw[i]).getActualName();
                return names;
            }
        }
        return new String[0];
    }

    static AnnotationDescription readAnnotation(AnnotationDescription annotation, String member) {
        for (MethodDescription.InDefinedShape m : annotation.getAnnotationType().getDeclaredMethods()) {
            if (m.getName().equals(member)) return (AnnotationDescription) annotation.getValue(m).resolve();
        }
        return null;
    }

    static AnnotationDescription[] readAnnotationArray(AnnotationDescription annotation, String member) {
        for (MethodDescription.InDefinedShape m : annotation.getAnnotationType().getDeclaredMethods()) {
            if (m.getName().equals(member)) {
                Object[] raw = annotation.getValue(m).resolve(Object[].class);   // elements are AnnotationDescription
                AnnotationDescription[] out = new AnnotationDescription[raw.length];
                for (int i = 0; i < raw.length; i++) out[i] = (AnnotationDescription) raw[i];
                return out;
            }
        }
        return new AnnotationDescription[0];
    }

}
