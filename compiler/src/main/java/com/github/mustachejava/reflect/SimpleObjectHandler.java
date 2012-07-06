package com.github.mustachejava.reflect;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Map;

import com.github.mustachejava.MustacheException;
import com.github.mustachejava.util.GuardException;
import com.github.mustachejava.util.Wrapper;

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
              Class sClass = scope.getClass();
              try {
                try {
                  return getMethod(sClass, name).invoke(scope);
                } catch (NoSuchMethodException e) {
                  String propertyname = name.substring(0, 1).toUpperCase() +
                          (name.length() > 1 ? name.substring(1) : "");
                  try {
                    return getMethod(sClass, "get" + propertyname).invoke(scope);
                  } catch (NoSuchMethodException e2) {
                    try {
                      return getMethod(sClass, "is" + propertyname).invoke(scope);
                    } catch (NoSuchMethodException e3) {
                      try {
                        return getField(sClass, name).get(scope);
                      } catch (NoSuchFieldException e4) {
                        // Not set
                      }
                    }
                  }
                }
              } catch (InvocationTargetException ie) {
                throw new MustacheException("Failed to get " + name + " from " + sClass, ie);
              } catch (IllegalAccessException iae) {
                throw new MustacheException(
                        "Set accessible failed to get " + name + " from " + sClass, iae);
              }
            } else {
              Object[] subscope = { scope };
              Wrapper wrapper = find(name.substring(0, index), subscope);
              if (wrapper != null) {
                scope = wrapper.call(subscope);
                subscope = new Object[] { scope };
                return find(name.substring(index +1), new Object[] { subscope }).call(subscope);
              }
            }
          }
        }
        return null;
      }
    };
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
