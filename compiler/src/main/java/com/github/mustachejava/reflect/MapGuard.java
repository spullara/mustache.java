package com.github.mustachejava.reflect;

import java.util.Map;

import com.google.common.base.Predicate;

import com.github.mustachejava.util.GuardException;
import com.github.mustachejava.util.Wrapper;

/**
* Created with IntelliJ IDEA.
* User: spullara
* Date: 4/13/12
* Time: 10:10 AM
* To change this template use File | Settings | File Templates.
*/
public class MapGuard implements Predicate<Object[]> {
  private final int scopeIndex;
  private final String name;
  private final boolean contains;
  private final Wrapper[] wrappers;

  public MapGuard(int scopeIndex, String name, boolean contains, Wrapper[] wrappers) {
    this.scopeIndex = scopeIndex;
    this.name = name;
    this.contains = contains;
    this.wrappers = wrappers;
  }

  @Override
  public boolean apply(Object[] objects) {
    Object scope = objects[scopeIndex];
    if (wrappers != null) {
      for (Wrapper wrapper : wrappers) {
        scope = wrapper.call(new Object[] { scope });
      }
    }
    if (scope instanceof Map) {
      Map map = (Map) scope;
      if (contains) {
        return map.containsKey(name);
      } else {
        return !map.containsKey(name);
      }
    }
    return true;
  }
}
