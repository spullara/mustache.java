package com.github.mustachejava.impl;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.concurrent.Future;
import java.util.logging.Logger;

import com.github.mustachejava.Mustache;
import com.github.mustachejava.ObjectHandler;
import com.github.mustachejava.util.MethodGuardException;
import com.github.mustachejava.util.MethodWrapper;

/**
 * Lookup objects using reflection and execute them the same way.
 * <p/>
 * User: sam
 * Date: 7/24/11
 * Time: 3:02 PM
 */
public class DefaultObjectHandler implements ObjectHandler {

  // Create a map if one doesn't already exist -- MapMaker.computerHashMap seems to be
  // very inefficient, had to improvise
  protected static Map<Class, Map<String, MethodWrapper>> cache = new HashMap<Class, Map<String, MethodWrapper>>() {
    public synchronized Map<String, MethodWrapper> get(Object c) {
      Map<String, MethodWrapper> o = super.get(c);
      if (o == null) {
        o = new HashMap<String, MethodWrapper>();
        put((Class) c, o);
      }
      return o;
    }
  };

  private static final Method MAP_METHOD;
  static {
    try {
      MAP_METHOD = Map.class.getMethod("get", Object.class);
    } catch (NoSuchMethodException e) {
      throw new AssertionError(e);
    }
  }

  public MethodWrapper find(String name, Object... scopes) {
    MethodWrapper methodWrapper = null;
    int length = scopes.length;
    Class[] guard = createGuard(scopes);
    NEXT:
    for (int i = length - 1; i >= 0; i--) {
      Object scope = scopes[i];
      if (scope == null) continue;
      List<MethodWrapper> methodWrappers = null;
      int dotIndex;
      String subname = name;
      while ((dotIndex = subname.indexOf('.')) != -1) {
        String lookup = subname.substring(0, dotIndex);
        subname = subname.substring(dotIndex + 1);
        methodWrapper = findWrapper(new Class[]{scope.getClass()}, scope, lookup);
        if (methodWrapper != null) {
          if (methodWrappers == null) methodWrappers = new ArrayList<MethodWrapper>();
          methodWrappers.add(methodWrapper);
          try {
            scope = methodWrapper.call(scope);
          } catch (MethodGuardException e) {
            throw new AssertionError(e);
          }
        } else {
          continue NEXT;
        }
        if (scope == null) return null;
      }
      MethodWrapper wrapper = findWrapper(guard, scope, subname);
      if (wrapper != null) {
        methodWrapper = wrapper;
        wrapper.setScope(i);
        if (methodWrappers != null) {
          wrapper.addWrappers(methodWrappers.toArray(new MethodWrapper[methodWrappers.size()]));
        }
        break;
      }
    }
    return methodWrapper;
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

  private MethodWrapper findWrapper(Class[] guard, Object scope, String name) {
    if (scope == null) return null;
    if (scope instanceof Map) {
      Map map = (Map) scope;
      if (map.get(name) == null) {
        return null;
      } else {
        return new MethodWrapper(guard, MAP_METHOD, name);
      }
    }
    Class aClass = scope.getClass();
    Map<String, MethodWrapper> members;
    // Don't overload methods in your contexts
    MethodWrapper member = null;
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

  @Override
  public Iterator iterate(Object object) {
    Iterator i;
    if (object instanceof Iterator) {
      return (Iterator) object;
    } else if (object instanceof Iterable) {
      i = ((Iterable) object).iterator();
    } else {
      if (object == null) return EMPTY.iterator();
      if (object instanceof Boolean) {
        if (!(Boolean) object) {
          return EMPTY.iterator();
        }
      }
      if (object instanceof String) {
        if (object.toString().equals("")) {
          return EMPTY.iterator();
        }
      }
      i = new SingleValueIterator(object);
    }
    return i;
  }

  public static MethodWrapper getMethod(Class[] guard, String name, Class aClass, Class... params) throws NoSuchMethodException {
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
    return new MethodWrapper(guard, member);
  }

  public static MethodWrapper getField(Class[] guard, String name, Class aClass) throws NoSuchFieldException {
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
    return new MethodWrapper(guard, member);
  }

  protected static class SingleValueIterator implements Iterator {
    private boolean done;
    private Object value;

    public SingleValueIterator(Object value) {
      this.value = value;
    }

    @Override
    public boolean hasNext() {
      return !done;
    }

    @Override
    public Object next() {
      if (!done) {
        done = true;
        return value;
      }
      throw new NoSuchElementException();
    }

    @Override
    public void remove() {
      done = true;
    }
  }

}
