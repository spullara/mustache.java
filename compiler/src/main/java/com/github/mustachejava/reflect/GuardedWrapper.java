package com.github.mustachejava.reflect;

import java.util.Arrays;
import java.util.List;

import com.google.common.base.Predicate;

import com.github.mustachejava.util.GuardException;
import com.github.mustachejava.util.Wrapper;

/**
 * Wrapper that guards.
 */
public class GuardedWrapper implements Wrapper {
  protected final Predicate[] guard;
  private int hashCode;

  public GuardedWrapper(Predicate[] guard) {
    this.guard = guard;
  }

  @Override
  public Object call(Object[] scopes) throws GuardException {
    guardCall(scopes);
    return null;
  }

  protected void guardCall(Object[] scopes) throws GuardException {
    for (Predicate predicate : guard) {
      if (!predicate.apply(scopes)) {
        throw new GuardException();
      }
    }
  }

  @Override
  public int hashCode() {
    if (hashCode == 0) {
      for (Predicate predicate : guard) {
        hashCode += hashCode * 43 + predicate.hashCode();
      }
      if (hashCode == 0) hashCode = 1;
    }
    return hashCode;
  }

  @Override
  public boolean equals(Object o) {
    if (o instanceof GuardedWrapper) {
      GuardedWrapper other = (GuardedWrapper) o;
      return (guard == null && other.guard == null) || Arrays.equals(other.guard, guard);
    }
    return false;
  }
}
