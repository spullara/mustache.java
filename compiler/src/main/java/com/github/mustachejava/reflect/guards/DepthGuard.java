package com.github.mustachejava.reflect.guards;

import com.github.mustachejava.reflect.Guard;

import javax.annotation.Nullable;

/**
 * Check that there are the same number of scope levels.
 */
public class DepthGuard implements Guard {
  protected final int length;

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
