package com.github.mustachejava.reflect;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.github.mustachejava.ObjectHandler;
import com.github.mustachejava.util.GuardException;
import com.github.mustachejava.util.Wrapper;

/**
 * Lookup objects using reflection and execute them the same way.
 * <p/>
 * User: sam
 * Date: 7/24/11
 * Time: 3:02 PM
 */
public class ReflectionObjectHandler implements ObjectHandler {

  private static final Method MAP_METHOD;
  static {
    try {
      MAP_METHOD = Map.class.getMethod("get", Object.class);
    } catch (NoSuchMethodException e) {
      throw new AssertionError(e);
    }
  }

  @Override
  public Wrapper find(String name, Object... scopes) {
    Wrapper wrapper = null;
    int length = scopes.length;
    Class[] guard = createGuard(scopes);
    NEXT:
    for (int i = length - 1; i >= 0; i--) {
      Object scope = scopes[i];
      if (scope == null) continue;
      List<Wrapper> wrappers = null;
      int dotIndex;
      String subname = name;
      while ((dotIndex = subname.indexOf('.')) != -1) {
        String lookup = subname.substring(0, dotIndex);
        subname = subname.substring(dotIndex + 1);
        wrapper = findWrapper(new Class[]{scope.getClass()}, scope, lookup);
        if (wrapper != null) {
          if (wrappers == null) wrappers = new ArrayList<Wrapper>();
          wrappers.add(wrapper);
          try {
            scope = wrapper.call(scope);
          } catch (GuardException e) {
            throw new AssertionError(e);
          }
        } else {
          continue NEXT;
        }
        if (scope == null) return null;
      }
      ReflectionWrapper foundWrapper = findWrapper(guard, scope, subname);
      if (foundWrapper != null) {
        foundWrapper.setScope(i);
        if (wrappers != null) {
          foundWrapper.addWrappers(wrappers.toArray(new Wrapper[wrappers.size()]));
        }
        wrapper = foundWrapper;
        break;
      }
    }
    return wrapper;
  }

  @Override
  public Object coerce(Object object) {
    return object;
  }

  private Class[] createGuard(Object... scopes) {
    int length = scopes.length;
    Class[] guard = new Class[length];
    for (int i = 0; i < length; i++) {
      Object scope = scopes[i];
      if (scope != null) {
        guard[i] = scope.getClass();
      }
    }
    return guard;
  }

  private ReflectionWrapper findWrapper(Class[] guard, Object scope, String name) {
    if (scope == null) return null;
    if (scope instanceof Map) {
      Map map = (Map) scope;
      if (map.get(name) == null) {
        return null;
      } else {
        return new ReflectionWrapper(guard, MAP_METHOD, name);
      }
    }
    Class aClass = scope.getClass();
    Map<String, Wrapper> members;
    // Don't overload methods in your contexts
    ReflectionWrapper member = null;
    try {
      member = getField(guard, name, aClass);
    } catch (NoSuchFieldException e) {
      // Not set
    }
    if (member == null) {
      try {
        member = getMethod(guard, name, aClass);
      } catch (NoSuchMethodException e) {
        try {
          member = getMethod(guard, name, aClass, List.class);
        } catch (NoSuchMethodException e1) {
          String propertyname = name.substring(0, 1).toUpperCase() +
                  (name.length() > 1 ? name.substring(1) : "");
          try {
            member = getMethod(guard, "get" + propertyname, aClass);
          } catch (NoSuchMethodException e2) {
            try {
              member = getMethod(guard, "is" + propertyname, aClass);
            } catch (NoSuchMethodException e3) {
              // Nothing to be done
            }
          }
        }
      }
    }
    return member;
  }

  public static ReflectionWrapper getMethod(Class[] guard, String name, Class aClass, Class... params) throws NoSuchMethodException {
    Method member;
    try {
      member = aClass.getDeclaredMethod(name, params);
    } catch (NoSuchMethodException nsme) {
      Class superclass = aClass.getSuperclass();
      if (superclass != Object.class) {
        return getMethod(guard, name, superclass, params);
      }
      throw nsme;
    }
    if ((member.getModifiers() & Modifier.PRIVATE) == Modifier.PRIVATE) {
      throw new NoSuchMethodException("Only public, protected and package members allowed");
    }
    member.setAccessible(true);
    return new ReflectionWrapper(guard, member);
  }

  public static ReflectionWrapper getField(Class[] guard, String name, Class aClass) throws NoSuchFieldException {
    Field member;
    try {
      member = aClass.getDeclaredField(name);
    } catch (NoSuchFieldException nsfe) {
      Class superclass = aClass.getSuperclass();
      if (superclass != Object.class) {
        return getField(guard, name, superclass);
      }
      throw nsfe;
    }
    if ((member.getModifiers() & Modifier.PRIVATE) == Modifier.PRIVATE) {
      throw new NoSuchFieldException("Only public, protected and package members allowed");
    }
    member.setAccessible(true);
    return new ReflectionWrapper(guard, member);
  }

}
