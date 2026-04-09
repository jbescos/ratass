package jsinterop.annotations;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface JsEnum {
    String name() default "<auto>";

    String namespace() default "<auto>";

    boolean isNative() default false;

    boolean hasCustomValue() default false;
}
