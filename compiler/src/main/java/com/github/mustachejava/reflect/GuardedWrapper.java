package com.github.mustachejava.reflect;

import java.util.List;

import com.google.common.base.Predicate;

import com.github.mustachejava.util.GuardException;
import com.github.mustachejava.util.Wrapper;

/**
 * Wrapper that guards.
 */
public class GuardedWrapper implements Wrapper {
  protected final List<? extends Predicate<Object[]>> guard;

  public GuardedWrapper(List<? extends Predicate<Object[]>> guard) {
    this.guard = guard;
  }

  @Override
  public Object call(Object[] scopes) throws GuardException {
    guardCall(scopes);
    return null;
  }

  protected void guardCall(Object[] scopes) throws GuardException {
    for (Predicate<Object[]> predicate : guard) {
      if (!predicate.apply(scopes)) {
        throw new GuardException();
      }
    }
  }
}
