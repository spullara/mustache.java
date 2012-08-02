package com.github.mustachejava.reflect;

import com.github.mustachejava.Binding;
import com.github.mustachejava.Code;
import com.github.mustachejava.MustacheException;
import com.github.mustachejava.TemplateContext;
import com.github.mustachejava.util.GuardException;
import com.github.mustachejava.util.Wrapper;

import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class SimpleObjectHandler extends BaseObjectHandler {

  @Override
  public Binding createBinding(final String name, TemplateContext tc, Code code) {
    return new Binding() {
      // We find the wrapper just once since only the name is needed
      private Wrapper wrapper = find(name, null);

      @Override
      public Object get(Object[] scopes) {
        return wrapper.call(scopes);
      }
    };
  }

  @Override
  public Wrapper find(final String name, final Object[] scopes) {
    return new Wrapper() {
      @Override
      public Object call(Object[] scopes) throws GuardException {
        for (int i = scopes.length - 1; i >= 0; i--) {
          Object scope = scopes[i];
          if (scope != null) {
            int index = name.indexOf(".");
            if (index == -1) {
              // Special case Maps
              if (scope instanceof Map) {
                Map map = (Map) scope;
                if (map.containsKey(name)) {
                  return map.get(name);
                }
              }
              // Check to see if there is a method or field that matches
              try {
                AccessibleObject ao = lookup(scope.getClass(), name);
                if (ao instanceof Method) {
                  return ((Method) ao).invoke(scope);
                } else if (ao instanceof Field) {
                  return ((Field) ao).get(scope);
                }
              } catch (InvocationTargetException ie) {
                throw new MustacheException("Failed to get " + name + " from " + scope.getClass(), ie);
              } catch (IllegalAccessException iae) {
                throw new MustacheException("Set accessible failed to get " + name + " from " + scope.getClass(), iae);
              }
            } else {
              // Dig into the dot-notation through recursion
              Object[] subscope = {scope};
              Wrapper wrapper = find(name.substring(0, index), subscope);
              if (wrapper != null) {
                scope = wrapper.call(subscope);
                subscope = new Object[]{scope};
                return find(name.substring(index + 1), new Object[]{subscope}).call(subscope);
              }
            }
          }
        }
        return null;
      }
    };
  }

  // Used for the member cache
  private static class WrapperKey {
    private final Class aClass;
    private final String name;
    private int hashcode;

    WrapperKey(Class aClass, String name) {
      this.aClass = aClass;
      this.name = name;
      hashcode = aClass.hashCode() + 43 * name.hashCode();
    }

    @Override
    public int hashCode() {
      return hashcode;
    }

    @Override
    public boolean equals(Object obj) {
      if (obj instanceof WrapperKey) {
        WrapperKey oKey = (WrapperKey) obj;
        return aClass.equals(oKey.aClass) && name.equals(oKey.name);
      } else {
        return false;
      }
    }
  }

  // Cache of classes + name => field mappings
  // By keeping this non-static you can release the cache by releasing the handler
  private Map<WrapperKey, AccessibleObject> cache = new ConcurrentHashMap<WrapperKey, AccessibleObject>();

  // Used to cache misses
  private static AccessibleObject NONE;
  static {
    try {
      NONE = SimpleObjectHandler.class.getDeclaredField("NONE");
    } catch (NoSuchFieldException e) {
      throw new AssertionError("Failed to init: " + e);
    }
  }

  // Use the cache to find lookup members faster
  private AccessibleObject lookup(Class sClass, String name) {
    WrapperKey key = new WrapperKey(sClass, name);
    AccessibleObject ao = cache.get(key);
    if (ao == null) {
      ao = findMember(sClass, name);
      cache.put(key, ao == null ? NONE : ao);
    }
    return ao == NONE ? null : ao;
  }

  @Override
  public String stringify(Object object) {
    return object.toString();
  }
}
