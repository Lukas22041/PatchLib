package patchlib.api.data;

import com.fs.starfarer.api.ModSpecAPI;

import java.util.List;

public interface ClassData {

    public String getName();
    public String getSuperName();
    public String getPackage();

    public List<String> getInterfaceNames();

    public List<MethodData> getMethods();
    public List<FieldData> getFields();

    public AnnotationData getAnnotation(String id);
    public List<AnnotationData> getAnnotations();

    public boolean isFromStarsector();

    public ModSpecAPI getSourceMod();
}
