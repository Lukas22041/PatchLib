package patchlib.agent.data;

import com.fs.starfarer.api.ModSpecAPI;
import net.bytebuddy.description.NamedElement;
import net.bytebuddy.description.type.PackageDescription;
import net.bytebuddy.description.type.TypeDescription;
import patchlib.api.data.AnnotationData;
import patchlib.api.data.ClassData;
import patchlib.api.data.FieldData;
import patchlib.api.data.MethodData;

import java.util.List;

public class ClassDataImpl implements ClassData {

    private TypeDescription typeDescription;
    private ModSpecAPI modSpec;

    public ClassDataImpl(TypeDescription typeDescription, ModSpecAPI modSpec) {
        this.typeDescription = typeDescription;
        this.modSpec = modSpec;
    }

    @Override
    public String getClassName() {
        return typeDescription.asErasure().getActualName();
    }

    @Override
    public String getSuperClassName() {
        TypeDescription.Generic description = typeDescription.getSuperClass();
        return description != null ? description.asErasure().getActualName() : null;
    }

    @Override
    public String getPackage() {
        PackageDescription description = typeDescription.getPackage();
        return description != null ? description.getActualName() : null;
    }

    @Override
    public List<String> getInterfaceNames() {
        return typeDescription.getInterfaces().stream().map(NamedElement::getActualName).toList();
    }

    /*@Override
    public List<MethodData> getMethods() {
        return List.of();
    }

    @Override
    public List<FieldData> getFields() {
        return List.of();
    }

    @Override
    public AnnotationData getAnnotation() {
        return null;
    }

    @Override
    public List<AnnotationData> getAnnotations() {
        return List.of();
    }

    @Override
    public ModSpecAPI getSourceMod() {
        return null;
    }*/
}
