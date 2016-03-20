package com.github.mustachejava;

import java.util.List;

/**
 * Bindings connect templates to their views.
 *
 * User: sam
 * Date: 7/7/12
 * Time: 6:07 PM
 */
public interface Binding {
  Object get(List<Object> scopes);
}
