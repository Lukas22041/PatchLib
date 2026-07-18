package patchlib.agent.data;

import net.bytebuddy.description.annotation.AnnotationDescription;
import net.bytebuddy.description.annotation.AnnotationList;
import net.bytebuddy.description.method.MethodDescription;
import patchlib.api.data.AnnotationData;
import patchlib.api.data.MethodData;

import java.util.ArrayList;
import java.util.List;

public class MethodDataImpl implements MethodData {

    private MethodDescription.InDefinedShape methodDescription;

    public MethodDataImpl(MethodDescription.InDefinedShape methodDescription) {
        this.methodDescription = methodDescription;
    }

    @Override
    public String getName() {
        return methodDescription.getActualName();
    }

    @Override
    public List<String> getParameterTypeNames() {
        return methodDescription.getParameters().stream()
                .map(param -> param.getType().asErasure().getActualName()).toList();
    }

    @Override
    public int getParameterCount() {
        return methodDescription.getParameters().size();
    }

    @Override
    public String getReturnTypeName() {
        return methodDescription.getReturnType().asErasure().getActualName();
    }

    @Override
    public boolean isPublic() {
        return methodDescription.isPublic();
    }

    @Override
    public boolean isPrivate() {
        return methodDescription.isPrivate();
    }

    @Override
    public boolean isProtected() {
        return methodDescription.isProtected();
    }

    @Override
    public boolean isPackagePrivate() {
        return methodDescription.isPackagePrivate();
    }

    @Override
    public boolean isStatic() {
        return methodDescription.isStatic();
    }

    @Override
    public boolean isConstructor() {
        return methodDescription.isConstructor();
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
        for (AnnotationDescription annotation : methodDescription.getDeclaredAnnotations()) {
            data.add(new AnnotationDataImpl(annotation));
        }
        return data;
    }
}
