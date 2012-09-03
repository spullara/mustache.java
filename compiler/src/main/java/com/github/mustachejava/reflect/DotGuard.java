package com.github.mustachejava.reflect;

import com.github.mustachejava.compile.CompilableGuard;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Label;
import org.objectweb.asm.commons.GeneratorAdapter;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Guard dot notation references.
 */
public class DotGuard implements CompilableGuard {

  private final String lookup;
  private final int scopeIndex;
  private final Class classGuard;

  public DotGuard(String lookup, int scopeIndex, Object classGuard) {
    this.lookup = lookup;
    this.scopeIndex = scopeIndex;
    this.classGuard = classGuard.getClass();
  }

  @Override
  public int hashCode() {
    return (lookup.hashCode() * 43 + scopeIndex) * 43 + classGuard.hashCode();
  }

  @Override
  public boolean equals(Object o) {
    if (o instanceof DotGuard) {
      DotGuard other = (DotGuard) o;
      return scopeIndex == other.scopeIndex && lookup.equals(other.lookup) && classGuard.equals(other.classGuard);
    }
    return false;
  }

  @Override
  public boolean apply(Object[] objects) {
    return true;
  }

  @Override
  public void addGuard(Label returnFalse, GeneratorAdapter gm, GeneratorAdapter cm, ClassWriter cw, AtomicInteger id, String className) {
    // do nothing and it is assumed true
  }
}
