package com.github.mustachejava.reflect;

import java.lang.reflect.AccessibleObject;
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

  protected static final Method MAP_METHOD;
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
        wrapper = findWrapper(0, null, new Class[] { scope.getClass() }, scope, lookup);
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
      Wrapper[] foundWrappers = wrappers == null ? null : wrappers.toArray(new Wrapper[wrappers.size()]);
      Wrapper foundWrapper = findWrapper(i, foundWrappers, guard, scope, subname);
      if (foundWrapper != null) {
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

  protected Class[] createGuard(Object... scopes) {
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

  protected Wrapper findWrapper(int scopeIndex, Wrapper[] wrappers, Class[] guard, Object scope, String name) {
    if (scope == null) return null;
    if (scope instanceof Map) {
      Map map = (Map) scope;
      if (map.get(name) == null) {
        return null;
      } else {
        return createWrapper(scopeIndex, wrappers, guard, MAP_METHOD, name);
      }
    }
    Class aClass = scope.getClass();
    // Don't overload methods in your contexts
    Wrapper member = null;
    try {
      member = getField(scopeIndex, wrappers, guard, name, aClass);
    } catch (NoSuchFieldException e) {
      // Not set
    }
    if (member == null) {
      try {
        member = getMethod(scopeIndex, wrappers, guard, name, aClass);
      } catch (NoSuchMethodException e) {
        try {
          member = getMethod(scopeIndex, wrappers, guard, name, aClass, List.class);
        } catch (NoSuchMethodException e1) {
          String propertyname = name.substring(0, 1).toUpperCase() +
                  (name.length() > 1 ? name.substring(1) : "");
          try {
            member = getMethod(scopeIndex, wrappers, guard, "get" + propertyname, aClass);
          } catch (NoSuchMethodException e2) {
            try {
              member = getMethod(scopeIndex, wrappers, guard, "is" + propertyname, aClass);
            } catch (NoSuchMethodException e3) {
              // Nothing to be done
            }
          }
        }
      }
    }
    return member;
  }

  protected Wrapper getMethod(int scopeIndex, Wrapper[] wrappers, Class[] guard, String name, Class aClass, Class... params) throws NoSuchMethodException {
    Method member;
    try {
      member = aClass.getDeclaredMethod(name, params);
    } catch (NoSuchMethodException nsme) {
      Class superclass = aClass.getSuperclass();
      if (superclass != null && superclass != Object.class) {
        return getMethod(scopeIndex, wrappers, guard, name, superclass, params);
      }
      throw nsme;
    }
    if ((member.getModifiers() & Modifier.PRIVATE) == Modifier.PRIVATE) {
      throw new NoSuchMethodException("Only public, protected and package members allowed");
    }
    member.setAccessible(true);
    return createWrapper(scopeIndex, wrappers, guard, member);
  }

  protected Wrapper getField(int scopeIndex, Wrapper[] wrappers, Class[] guard, String name, Class aClass) throws NoSuchFieldException {
    Field member;
    try {
      member = aClass.getDeclaredField(name);
    } catch (NoSuchFieldException nsfe) {
      Class superclass = aClass.getSuperclass();
      if (superclass != null && superclass != Object.class) {
        return getField(scopeIndex, wrappers, guard, name, superclass);
      }
      throw nsfe;
    }
    if ((member.getModifiers() & Modifier.PRIVATE) == Modifier.PRIVATE) {
      throw new NoSuchFieldException("Only public, protected and package members allowed");
    }
    member.setAccessible(true);
    return createWrapper(scopeIndex, wrappers, guard, member);
  }

  protected Wrapper createWrapper(int scopeIndex, Wrapper[] wrappers, Class[] guard, AccessibleObject member, Object... arguments) {
    return new ReflectionWrapper(scopeIndex, wrappers, guard, member, arguments);
  }

}
