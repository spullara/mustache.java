package com.github.mustachejava.util;

import java.util.List;

/**
 * Call a wrapped name on a set of scopes.
 */
public interface Wrapper {
  Object call(List<Object> scopes) throws GuardException;
}
