package com.github.mustachejava;

import com.github.mustachejava.util.IndentWriter;

import java.io.Writer;
import java.util.List;

/**
 * This is the callback interface for iterating on a value. You can override the iterate
 * method in an ObjectHandler to change the types recognized by mustache.java as iterable.
 */
public interface Iteration {
  IndentWriter next(IndentWriter writer, Object next, List<Object> scopes);
}
