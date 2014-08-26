package nl.onedott.mustachejava.extension.springmvc.annotation;

import nl.onedott.mustachejava.extension.springmvc.config.MustacheConfiguration;
import org.springframework.context.annotation.Import;

import java.lang.annotation.*;

/**
 * Enable Mustache views and model processing for Spring MVC.
 *
 * @author Bart Tegenbosch
 */
@Documented
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Import(MustacheConfiguration.class)
public @interface EnableMustache {
    /*
     * The resource location for mustache templates.
     * Supports resource prefixes like {@code file:} and {@code classpath:}
     */
    String value() default "mustache/";
}
