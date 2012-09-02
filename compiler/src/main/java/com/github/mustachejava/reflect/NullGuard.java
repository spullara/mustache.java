package com.github.mustachejava.reflect;

public class NullGuard implements Guard {
  @Override
  public boolean apply(Object[] objects) {
    return objects[0] == null;
  }
}
