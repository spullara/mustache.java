package com.github.mustachejava.util;

/**
 * Call a wrapped name on a set of scopes.
 */
public interface Wrapper {
  public static final Wrapper NULL_WRAPPER = new Wrapper() {
    @Override
    public Object call(Object[] scopes) throws GuardException {
      return null;
    }
  };
  Object call(Object[] scopes) throws GuardException;
}
