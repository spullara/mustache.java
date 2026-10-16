package com.github.mustachejava.reflect.guards;

import com.github.mustachejava.reflect.Guard;

import java.util.List;

public value class NullGuard implements Guard {
  @Override
  public boolean apply(List<Object> objects) {
    return objects.get(0) == null;
  }

  public String toString() {
    return "[NullGuard]";
  }

}
