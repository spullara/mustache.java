package com.github.mustachejava;

import java.io.Writer;

/**
 * This is the callback interface for iterating on a value. You can override the iterate
 * method in an ObjectHandler to change the types recognized by mustache.java as iterable.
 */
public interface Iteration {
  Writer next(Writer writer, Object next, Object[] scopes);
}
