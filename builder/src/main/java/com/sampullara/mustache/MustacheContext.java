package com.sampullara.mustache;

import java.io.BufferedReader;

/**
 * This gives you the ability to override the default behavior
 * of the Mustache builder by allowing you to find streams based
 * on names.
 */
public interface MustacheContext {
  BufferedReader getReader(String name) throws MustacheException;
}
