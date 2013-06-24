package com.github.mustachejava.reflect.guards;

import com.github.mustachejava.reflect.Guard;

public class NullGuard implements Guard {
  @Override
  public boolean apply(Object[] objects) {
    return objects[0] == null;
  }

  public String toString() {
    return "[NullGuard]";
  }

}
