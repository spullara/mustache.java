package com.github.mustachejava.reflect.guards;

import com.github.mustachejava.reflect.Guard;

public class NullGuard implements Guard {
  public boolean apply(Object[] objects) {
    return objects[0] == null;
  }
}
