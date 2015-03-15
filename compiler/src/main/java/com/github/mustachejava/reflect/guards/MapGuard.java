package com.github.mustachejava.reflect.guards;

import com.github.mustachejava.ObjectHandler;
import com.github.mustachejava.reflect.Guard;
import com.github.mustachejava.util.Wrapper;

import java.util.List;
import java.util.Map;

import static com.github.mustachejava.reflect.ReflectionObjectHandler.unwrap;

/**
 * Guards whether or not a name was present in the map.
 */
public class MapGuard implements Guard {
  protected final ObjectHandler oh;
  protected final int scopeIndex;
  protected final String name;
  protected final boolean contains;
  protected final Wrapper[] wrappers;

  public MapGuard(ObjectHandler oh, int scopeIndex, String name, boolean contains, Wrapper[] wrappers) {
    this.oh = oh;
    this.scopeIndex = scopeIndex;
    this.name = name;
    this.contains = contains;
    this.wrappers = wrappers;
  }

  @Override
  public boolean apply(List<Object> objects) {
    Object scope = unwrap(oh, scopeIndex, wrappers, objects);
    if (scope instanceof Map) {
      Map map = (Map) scope;
      if (contains) {
        return map.containsKey(name);
      } else {
        return !map.containsKey(name);
      }
    }
    return false;
  }

  public String toString() {
    return "[MapGuard: " + scopeIndex + " " + name + " " + contains + "]";
  }

}
