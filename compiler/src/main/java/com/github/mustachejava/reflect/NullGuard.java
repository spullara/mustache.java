package com.github.mustachejava.reflect;

import com.google.common.base.Predicate;

import javax.annotation.Nullable;

public class NullGuard implements Predicate<Object[]> {
  @Override
  public boolean apply(Object[] objects) {
    return objects[0] == null;
  }
}
