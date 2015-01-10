package com.github.mustachejava;

/**
 * Bindings connect templates to their views.
 *
 * User: sam
 * Date: 7/7/12
 * Time: 6:07 PM
 */
public interface Binding {
  public Object get(Object[] scopes);
}
