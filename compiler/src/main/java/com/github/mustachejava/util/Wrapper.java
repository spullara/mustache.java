package com.github.mustachejava.util;

/**
 * Call a wrapped name on a set of scopes.
 */
public interface Wrapper {
  Object call(Object[] scopes) throws GuardException;
}
