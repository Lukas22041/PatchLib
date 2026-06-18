package patch_lib.api;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.CLASS)
public @interface MethodMatch {
    String methodName() default "";

    Class<?>[] parameters() default {};
    String[] parameterNames() default {};
    int parameterCount() default -1;

    Class<?> returnType() default Unset.class;
    String returnTypeName() default "";

    MethodType methodType() default MethodType.ANY;
    boolean staticOnly() default false;

}
