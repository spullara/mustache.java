/*
 * Copyright (c) 2014, Onedott, bart@onedott.nl
 * All rights reserved.
 */

package nl.onedott.mustachejava.extension.springmvc.config.annotation;

import org.springframework.context.annotation.Import;

import java.lang.annotation.*;

/**
 * Enable Mustache views for Spring MVC.
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
