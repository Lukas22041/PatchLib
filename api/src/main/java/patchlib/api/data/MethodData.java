package patchlib.api.data;

import java.util.List;

public interface MethodData {

    public String getName();

    public List<String> getParameterTypeNames();
    public int getParameterCount();

    public String getReturnTypeName();

    public boolean isPublic();
    public boolean isPrivate();
    public boolean isProtected();
    public boolean isPackagePrivate();

    public boolean isStatic();
    public boolean isConstructor();

    public AnnotationData getAnnotation(String id);
    public List<AnnotationData> getAnnotations();

}
