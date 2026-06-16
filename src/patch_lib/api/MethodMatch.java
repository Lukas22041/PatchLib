package patch_lib.api;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.CLASS)
public @interface MethodMatch {
    String targetMethodName() default "";

    Class<?>[] parameters() default {};
    String[] parameterNames() default {};
    int parameterCount() default -1;

    Class<?> returnType() default UnsetReturnType.class;
    String returnTypeName() default "";

    MethodType methodType() default MethodType.ANY;
    boolean staticOnly() default false;

}
