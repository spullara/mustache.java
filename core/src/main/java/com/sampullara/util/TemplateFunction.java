package com.sampullara.util;

import com.google.common.base.Function;

/**
 * Use this function if you want to transform template code rather than the results
 * of template code.
 */
public interface TemplateFunction extends Function<String, String> {
}
