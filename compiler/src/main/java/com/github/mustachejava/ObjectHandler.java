package com.github.mustachejava;

import com.github.mustachejava.util.InternalArrayList;
import com.github.mustachejava.util.Wrapper;

import java.io.Writer;
import java.util.List;

/**
 * The ObjectHandler is responsible for creating wrappers to find values
 * in scopes at runtime and to coerce those results to the appropriate Java types
 */
public interface ObjectHandler {
  /**
   * Find a value named "name" in the array of scopes in reverse order.
   * 
   * @param name the variable name
   * @param scopes the ordered list of scopes
   * @return a wrapper that can be used to extract a value
   */
  Wrapper find(String name, List<Object> scopes);

  /**
   * Coerce results to Java native iterables, functions, callables.
   * 
   * @param object transform an unknown type to a known type
   * @return the new object
   */
  Object coerce(Object object);

  /**
   * Iterate over an object by calling Iteration.next for each value.
   *
   * @param iteration callback for the next iteration
   * @param writer the writer to write to
   * @param object the current object
   * @param scopes the scopes present
   * @return the current writer
   */
  Writer iterate(Iteration iteration, Writer writer, Object object, List<Object> scopes);

  /**
   * Call Iteration.next() either 0 (true) or 1 (fale) times.
   *
   * @param iteration callback for the next iteration
   * @param writer the writer to write to
   * @param object the current object
   * @param scopes the scopes present
   * @return the current writer
   */
  Writer falsey(Iteration iteration, Writer writer, Object object, List<Object> scopes);

  /**
   * Each call site has its own binding to allow for fine grained caching without
   * a separate parallel hierarchy of objects.
   *
   * @param name the name that we bound
   * @param tc the textual context of the binding site
   * @param code the code that was bound
   * @return the binding
   */
  Binding createBinding(String name, TemplateContext tc, Code code);
  
  /**
   * Turns an object into the string representation that should be displayed
   * in templates.
   *
   * @param object the object to be displayed
   * @return a string representation of the object.
   */
  String stringify(Object object);
  
  static List<Object> makeList(Object scope) {
    List<Object> scopes = new InternalArrayList<>();
    scopes.add(scope);
    return scopes;
  }
}
