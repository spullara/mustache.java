package com.github.mustachejava.reflect.guards;

import com.github.mustachejava.reflect.Guard;

import java.util.List;

/**
 * Ensure that the class of the current scope is that same as when this wrapper was generated.
 * User: spullara
 * Date: 4/13/12
 * Time: 9:23 AM
 * To change this template use File | Settings | File Templates.
 */
public class ClassGuard implements Guard {
  protected final Class classGuard;
  protected final int scopeIndex;

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
    if (o instanceof ClassGuard) {
      ClassGuard other = (ClassGuard) o;
      return classGuard == null ? other.classGuard == null : classGuard.equals(other.classGuard);
    } else {
      return false;
    }
  }

  @Override
  public boolean apply(List<Object> scopes) {
    if (scopes == null || scopes.size() <= scopeIndex) return false;
    Object scope = scopes.get(scopeIndex);
    return !(scope != null && classGuard != scope.getClass()) && !(scope == null && classGuard != null);
  }

  public String toString() {
    return "[ClassGuard: " + scopeIndex + " " + classGuard.getName() + "]";
  }
}
