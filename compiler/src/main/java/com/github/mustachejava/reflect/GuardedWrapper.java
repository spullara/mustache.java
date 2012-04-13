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
  private Predicate[] predicates;
  private int hashCode;
  private int size;

  public GuardedWrapper(List<? extends Predicate<Object[]>> guard) {
    this.guard = guard;
  }

  @Override
  public Object call(Object[] scopes) throws GuardException {
    guardCall(scopes);
    return null;
  }

  protected void guardCall(Object[] scopes) throws GuardException {
    if (predicates == null) {
      size = guard.size();
      predicates = new Predicate[size];
      int size = guard.size();
      for (int i = 0; i < size; i++) {
        Predicate<Object[]> predicate = guard.get(i);
        predicates[i] = predicate;
        if (!predicate.apply(scopes)) {
          throw new GuardException();
        }
      }
    } else {
      int length = size;
      for (int i = 0; i < length; i++) {
        if (!predicates[i].apply(scopes)) {
          throw new GuardException();
        }
      }
    }
  }

  @Override
  public int hashCode() {
    if (hashCode == 0) {
      for (Predicate<Object[]> predicate : guard) {
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
      return (guard == null && other.guard == null) || other.guard.equals(guard);
    }
    return false;
  }
}
