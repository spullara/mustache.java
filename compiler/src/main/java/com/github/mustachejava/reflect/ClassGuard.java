package com.github.mustachejava.reflect;

import com.google.common.base.Predicate;

/**
 * Ensure that the class of the current scope is that same as when this wrapper was generated.
 * User: spullara
 * Date: 4/13/12
 * Time: 9:23 AM
 * To change this template use File | Settings | File Templates.
 */
public class ClassGuard implements Predicate<Object[]> {
  private final Class classGuard;
  private final int scopeIndex;

  public ClassGuard(int scopeIndex, Object scope) {
    this.scopeIndex = scopeIndex;
    this.classGuard = scope == null ? null : scope.getClass();
  }

  @Override
  public int hashCode() {
    return classGuard == null ? 0 : classGuard.hashCode();
  }

  @Override
  public boolean equals(Object o) {
    ClassGuard other = (ClassGuard) o;
    return o instanceof ClassGuard && (classGuard == null ? classGuard == other.classGuard : classGuard.equals(other.classGuard));
  }

  @Override
  public boolean apply(Object[] scopes) {
    if (scopes == null || scopes.length <= scopeIndex) return false;
    Object scope = scopes[scopeIndex];
    return (scope == null && classGuard == null) || (scope != null && classGuard == scope.getClass());
  }
}
