package com.github.mustachejava.reflect;

import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Label;
import org.objectweb.asm.commons.GeneratorAdapter;

import javax.annotation.Nullable;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Check that there are the same number of scope levels.
 */
public class DepthGuard implements CompilableGuard {
  private final int length;

  public DepthGuard(int length) {
    this.length = length;
  }

  @Override
  public int hashCode() {
    return length;
  }

  @Override
  public boolean equals(Object o) {
    if (o instanceof DepthGuard) {
      DepthGuard depthGuard = (DepthGuard) o;
      return length == depthGuard.length;
    }
    return false;
  }

  @Override
  public boolean apply(@Nullable Object[] objects) {
    return objects != null && length == objects.length;
  }

  @Override
  public void addGuard(Label returnFalse, GeneratorAdapter gm, GeneratorAdapter sm, ClassWriter cw, AtomicInteger id, String className) {
    // If objects is null return false
    gm.loadArg(0);
    gm.ifNull(returnFalse);

    // If they are not equal return false
    gm.loadArg(0);
    gm.arrayLength();
    gm.push(length);
    gm.ifICmp(GeneratorAdapter.NE, returnFalse);
  }
}
