package com.github.mustachejava.reflect;

import javax.annotation.Nullable;

import com.google.common.base.Predicate;

/**
 * Check that there are the same number of scope levels.
 */
public class DepthGuard implements Predicate<Object[]> {
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
}
