package nl.onedott.mustachejava.extension.springmvc.annotation;

import java.lang.annotation.*;

/**
 * @author Bart Tegenbosch
 */
@Documented
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Message {

}
