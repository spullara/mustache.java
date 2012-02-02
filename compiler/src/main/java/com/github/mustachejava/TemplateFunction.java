package com.github.mustachejava;

import com.google.common.base.Function;

/**
 * This function is required if you want the opportunity to change
 * the template vs just changing the output of the template.
 */
public interface TemplateFunction extends Function<String, String> {
}
