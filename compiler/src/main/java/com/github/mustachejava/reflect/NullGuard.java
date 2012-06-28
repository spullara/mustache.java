package com.github.mustachejava.reflect;

import javax.annotation.Nullable;

import com.google.common.base.Predicate;

public class NullGuard implements Predicate<Object[]> {
  @Override
  public boolean apply(@Nullable Object[] objects) {
    return objects[0] == null;
  }
}
