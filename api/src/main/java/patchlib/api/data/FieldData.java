package patchlib.api.data;

import java.util.List;

public interface FieldData {


    public String getName();
    public String getTypeName();

    public boolean isPublic();
    public boolean isPrivate();
    public boolean isProtected();
    public boolean isPackagePrivate();

    public boolean isStatic();

    public AnnotationData getAnnotation(String id);
    public List<AnnotationData> getAnnotations();

}
