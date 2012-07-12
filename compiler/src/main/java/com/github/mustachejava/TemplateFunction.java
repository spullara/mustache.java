package com.github.mustachejava;

import com.google.common.base.Function;

/**
 * Use this function if you to implement additional functions/lambdas
 * (eg. `{{#func1}}`) and want mustache.java to reparse their results again.
 */
public interface TemplateFunction extends Function<String, String> {
}
