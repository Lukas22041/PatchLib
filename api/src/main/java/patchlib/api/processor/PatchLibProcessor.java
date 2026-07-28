package patchlib.api.processor;

import patchlib.api.context.*;
import patchlib.api.patch.*;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.*;
import javax.tools.Diagnostic;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@SupportedAnnotationTypes({
        "patchlib.api.patch.After",
        "patchlib.api.patch.Before",
        "patchlib.api.patch.Except",

        "patchlib.api.patch.Patch",

        "patchlib.api.patch.RedirectCall",
        "patchlib.api.patch.RedirectFieldRead",
        "patchlib.api.patch.RedirectFieldWrite",
        "patchlib.api.patch.RedirectNew",
})
@SupportedSourceVersion(SourceVersion.RELEASE_17)
public class PatchLibProcessor extends AbstractProcessor {

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {

        /*for (Element element : roundEnv.getElementsAnnotatedWith(Patch.class)) {
            validatePatch(element);
        }*/

        for (Element element : roundEnv.getElementsAnnotatedWith(After.class)) {
            validateHandler(element, After.class, AfterContext.class);
        }

        for (Element element : roundEnv.getElementsAnnotatedWith(Before.class)) {
            validateHandler(element, Before.class, BeforeContext.class);
        }

        for (Element element : roundEnv.getElementsAnnotatedWith(Except.class)) {
            validateHandler(element, Except.class, ExceptContext.class);
        }

        for (Element element : roundEnv.getElementsAnnotatedWith(RedirectCall.class)) {
            validateHandler(element, RedirectCall.class, MethodCallContext.class);
        }

        for (Element element : roundEnv.getElementsAnnotatedWith(RedirectFieldRead.class)) {
            validateHandler(element, RedirectFieldRead.class, FieldReadContext.class);
        }

        for (Element element : roundEnv.getElementsAnnotatedWith(RedirectFieldWrite.class)) {
            validateHandler(element, RedirectFieldWrite.class, FieldWriteContext.class);
        }

        for (Element element : roundEnv.getElementsAnnotatedWith(RedirectNew.class)) {
            validateHandler(element, RedirectNew.class, ConstructorCallContext.class);
        }

        return false;
    }

    public void validateHandler(Element element, Class<?> annotation, Class<?> requiredContext) {

        if (element.getKind() != ElementKind.METHOD) {
            processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR,
                    "@" + annotation.getSimpleName() + " annotations can only be used on methods.", element);
            return;
        }

        ExecutableElement method = (ExecutableElement) element;

        if (!method.getModifiers().contains(Modifier.STATIC)) {
            processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR,
                    "@" + annotation.getSimpleName() + " patches need to be used with static methods. If this is kotlin code, make sure to also annotate it with @JvmStatic.", element);
        }

        if (method.getEnclosingElement().getAnnotation(Patch.class) == null) {
            processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR,
                    "@" + annotation.getSimpleName() + " patch handlers require a @" + Patch.class.getSimpleName() + " annotation on the patch class.", element);
        }

        boolean lacksContextParameter = method.getParameters().size() != 1 ||
                !method.getParameters().get(0).asType().toString().equals(requiredContext.getName());

        if (lacksContextParameter) {
            processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR,
                    "@" + annotation.getSimpleName() + " patches require the method to have a parameter of the \"" + requiredContext.getSimpleName() + "\" type. No other parameters are allowed", element);
        }
    }


}
