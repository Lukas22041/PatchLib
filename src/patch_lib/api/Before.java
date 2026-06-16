package patch_lib.api;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.CLASS)
public @interface Before {
    String targetMethodName() default "";

    Class<?>[] parameters() default {};
    String[] parameterNames() default {};
    int parameterCount() default -1;

    Class<?> returnType() default UnsetReturnType.class;
    String returnTypeName() default "";

    /** Order in which patches are executed, lower numbers are run first.*/
    int priority() default 0;

    MethodType methodType() default MethodType.ANY;
    boolean staticOnly() default false;

}
