package com.github.mustachejava.reflect;

import com.github.mustachejava.util.GuardException;
import com.github.mustachejava.util.Wrapper;

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
  protected final Guard[] guards;
  protected final Guard[] optimized;

  // Hashcode cache
  private int hashCode;

  public GuardedWrapper(Guard[] guards) {
    this.guards = guards;
    Guard[] optimized = CompilableGuard.Compiler.compile(guards);
    this.optimized = optimized.length < guards.length ? optimized : guards;
  }

  @Override
  public Object call(Object[] scopes) throws GuardException {
    guardCall(scopes);
    return null;
  }

  protected void guardCall(Object[] scopes) throws GuardException {
    for (Guard predicate : optimized) {
      if (!predicate.apply(scopes)) {
        throw guardException;
      }
    }
  }

  @Override
  public int hashCode() {
    if (hashCode == 0) {
      for (Guard predicate : guards) {
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
      return (guards == null && other.guards == null) || Arrays.equals(other.guards, guards);
    }
    return false;
  }
}
