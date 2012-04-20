package com.github.mustachejava.reflect;

import com.github.mustachejava.util.GuardException;
import com.github.mustachejava.util.Wrapper;
import com.google.common.base.Predicate;

import java.util.Arrays;

/**
 * Wrapper that guards.
 */
public class GuardedWrapper implements Wrapper {
  // We only need a single guard exception -- don't fill stack trace
  // and don't reallocate it.
  private static final GuardException guardException = new GuardException();
  static {
    guardException.setStackTrace(new StackTraceElement[0]);
  }

  // Array of guards that must be satisfied
  protected final Predicate[] guard;

  // Hashcode cache
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
        throw guardException;
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
