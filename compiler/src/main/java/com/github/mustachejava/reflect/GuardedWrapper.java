package com.github.mustachejava.reflect;

import com.github.mustachejava.util.GuardException;
import com.github.mustachejava.util.Wrapper;

/**
 * Wrapper that guards.
 */
public class GuardedWrapper implements Wrapper {
  protected final Class[] guard;

  public GuardedWrapper(Class[] guard) {
    this.guard = guard;
  }

  public GuardedWrapper(Object[] scopes) {
    int length = scopes.length;
    guard = new Class[length];
    for (int i = 0; i < length; i++) {
      Object scope = scopes[i];
      if (scope != null) {
       guard[i] = scope.getClass();
      }
    }
  }

  @Override
  public Object call(Object[] scopes) throws GuardException {
    guardCall(scopes);
    return null;
  }

  protected void guardCall(Object[] scopes) throws GuardException {
    int length = scopes.length;
    if (guard.length != length) {
      throw new GuardException();
    }
    for (int j = 0; j < length; j++) {
      Class guardClass = guard[j];
      if (guardClass != null && !guardClass.isInstance(scopes[j])) {
        throw new GuardException();
      }
    }
  }
}
