package patchlib.api.data;

public interface AnnotationData {

    public String getName();

    public String getClassName(String id);
    public String[] getClassNameArray(String id);

    public String getString(String id);
    public String[] getStringArray(String id);

    public Integer getInt(String id);
    public Integer[] getIntArray(String id);

    public Float getFloat(String id);
    public Float[] getFloatArray(String id);

    public String getEnumValue(String id);

    public AnnotationData getAnnotation(String id);
    public AnnotationData[] getAnnotationArray(String id);
}
