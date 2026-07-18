package patchlib.agent.data;

import net.bytebuddy.description.annotation.AnnotationDescription;
import net.bytebuddy.description.field.FieldDescription;
import patchlib.api.data.AnnotationData;
import patchlib.api.data.FieldData;

import java.util.ArrayList;
import java.util.List;

public class FieldDataImpl implements FieldData {

    private FieldDescription fieldDescription;

    public FieldDataImpl(FieldDescription fieldDescription) {
        this.fieldDescription = fieldDescription;
    }

    @Override
    public String getName() {
        return fieldDescription.getActualName();
    }

    @Override
    public String getTypeName() {
        return fieldDescription.getType().asErasure().getActualName();
    }

    @Override
    public boolean isPublic() {
        return fieldDescription.isPublic();
    }

    @Override
    public boolean isPrivate() {
        return fieldDescription.isPrivate();
    }

    @Override
    public boolean isProtected() {
        return fieldDescription.isProtected();
    }

    @Override
    public boolean isPackagePrivate() {
        return fieldDescription.isPackagePrivate();
    }

    @Override
    public boolean isStatic() {
        return fieldDescription.isStatic();
    }

    @Override
    public AnnotationData getAnnotation(String id) {
        for (AnnotationData annotation : getAnnotations()) {
            if (annotation.getName().equals(id)) {
                return annotation;
            }
        }
        return null;
    }

    @Override
    public List<AnnotationData> getAnnotations() {
        ArrayList<AnnotationData> data = new ArrayList<>();
        for (AnnotationDescription annotation : fieldDescription.getDeclaredAnnotations()) {
            data.add(new AnnotationDataImpl(annotation));
        }
        return data;
    }
}
