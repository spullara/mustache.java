package com.github.mustachejava.reflect;

import com.github.mustachejava.MustacheException;
import com.github.mustachejava.util.GuardException;
import com.github.mustachejava.util.Wrapper;

import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class SimpleObjectHandler extends BaseObjectHandler {
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
              if (scope instanceof Map) {
                Map map = (Map) scope;
                if (map.containsKey(name)) {
                  return map.get(name);
                }
              }
              final Class sClass = scope.getClass();
              try {
                AccessibleObject ao = lookup(sClass, name);
                if (ao instanceof Method) {
                  return ((Method) ao).invoke(scope);
                } else if (ao instanceof Field) {
                  return ((Field) ao).get(scope);
                }
              } catch (InvocationTargetException ie) {
                throw new MustacheException("Failed to get " + name + " from " + sClass, ie);
              } catch (IllegalAccessException iae) {
                throw new MustacheException(
                        "Set accessible failed to get " + name + " from " + sClass, iae);
              }
            } else {
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

  private static AccessibleObject NONE;
  static {
    try {
      NONE = SimpleObjectHandler.class.getDeclaredField("NONE");
    } catch (NoSuchFieldException e) {
      throw new AssertionError("Failed to init: " + e);
    }
  }

  private AccessibleObject lookup(Class sClass, String name) {
    WrapperKey key = new WrapperKey(sClass, name);
    AccessibleObject ao = cache.get(key);
    if (ao == null) {
      try {
        ao = getMethod(sClass, name);
      } catch (NoSuchMethodException e) {
        String propertyname = name.substring(0, 1).toUpperCase() +
                (name.length() > 1 ? name.substring(1) : "");
        try {
          ao = getMethod(sClass, "get" + propertyname);
        } catch (NoSuchMethodException e2) {
          try {
            ao = getMethod(sClass, "is" + propertyname);
          } catch (NoSuchMethodException e3) {
            try {
              ao = getField(sClass, name);
            } catch (NoSuchFieldException e4) {
              ao = NONE;
            }
          }
        }
      }
      cache.put(key, ao);
    }
    return ao == NONE ? null : ao;
  }

  protected Field getField(Class aClass, String name) throws NoSuchFieldException {
    Field member;
    try {
      member = aClass.getDeclaredField(name);
    } catch (NoSuchFieldException nsfe) {
      Class superclass = aClass.getSuperclass();
      if (superclass != null && superclass != Object.class) {
        return getField(superclass, name);
      }
      throw nsfe;
    }
    checkField(member);
    member.setAccessible(true);
    return member;
  }

  private static class WrapperKey {
    private final Class aClass;
    private final String name;

    WrapperKey(Class aClass, String name) {
      this.aClass = aClass;
      this.name = name;
    }

    @Override
    public int hashCode() {
      return aClass.hashCode() + 43 * name.hashCode();
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

  private Map<WrapperKey, AccessibleObject> cache = new ConcurrentHashMap<WrapperKey, AccessibleObject>();

  protected Method getMethod(Class aClass, String name) throws NoSuchMethodException {
    Method member;
    try {
      member = aClass.getDeclaredMethod(name, new Class[0]);
    } catch (NoSuchMethodException nsme) {
      Class superclass = aClass.getSuperclass();
      if (superclass != null && superclass != Object.class) {
        return getMethod(superclass, name);
      }
      throw nsme;
    }
    checkMethod(member);
    member.setAccessible(true);
    return member;
  }

  // We default to not allowing private methods
  protected void checkMethod(Method member) throws NoSuchMethodException {
    if ((member.getModifiers() & Modifier.PRIVATE) == Modifier.PRIVATE) {
      throw new NoSuchMethodException("Only public, protected and package members allowed");
    }
  }

  // We default to not allowing private fields
  protected void checkField(Field member) throws NoSuchFieldException {
    if ((member.getModifiers() & Modifier.PRIVATE) == Modifier.PRIVATE) {
      throw new NoSuchFieldException("Only public, protected and package members allowed");
    }
  }
}
