package com.sampullara.mustache;

import java.io.Reader;

/**
 * This gives you the ability to override the default behavior
 * of the Mustache builder by allowing you to find streams based
 * on names.
 */
public interface MustacheContext {
  Reader getReader(String name) throws MustacheException;
}
