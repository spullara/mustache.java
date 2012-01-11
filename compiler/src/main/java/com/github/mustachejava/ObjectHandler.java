package com.github.mustachejava;

import com.github.mustachejava.util.Wrapper;

/**
 * The ObjectHandler is responsible for creating wrappers to find values
 * in scopes at runtime and to coerce those results to the appropriate Java types
 */
public interface ObjectHandler {
  // Find methods to call
  Wrapper find(String name, Object... scopes);

  // Coerce results to Java native iterables, functions, callables
  Object coerce(Object object);
}
