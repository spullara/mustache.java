package com.github.mustachejava;

import com.github.mustachejava.util.Wrapper;

import java.io.Writer;

/**
 * The ObjectHandler is responsible for creating wrappers to find values
 * in scopes at runtime and to coerce those results to the appropriate Java types
 */
public interface ObjectHandler {
  /**
   * Find a value named "name" in the array of scopes in reverse order.
   * @param name
   * @param scopes
   * @return
   */
  Wrapper find(String name, Object[] scopes);

  /**
   * Coerce results to Java native iterables, functions, callables.
   * @param object
   * @return
   */
  Object coerce(Object object);

  /**
   * Iterate over an object by calling Iteration.next for each value.
   * @param iteration
   * @param writer
   * @param object
   * @param scopes
   * @return
   */
  Writer iterate(Iteration iteration, Writer writer, Object object, Object[] scopes);

  /**
   * Call Iteration.next() either 0 (true) or 1 (fale) times.
   * @param iteration
   * @param writer
   * @param object
   * @param scopes
   * @return
   */
  Writer falsey(Iteration iteration, Writer writer, Object object, Object[] scopes);

  /**
   * Each call site has its own binding to allow for fine grained caching.
   * @param name
   * @param tc
   * @param code
   * @return
   */
  Binding createBinding(String name, TemplateContext tc, Code code);
}
