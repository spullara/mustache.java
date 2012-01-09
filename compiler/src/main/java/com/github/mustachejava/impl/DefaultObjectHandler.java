package com.github.mustachejava.impl;

import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.github.mustachejava.Mustache;
import com.github.mustachejava.ObjectHandler;

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
  protected static Map<Class, Map<String, AccessibleObject>> cache = new HashMap<Class, Map<String, AccessibleObject>>() {
    public synchronized Map<String, AccessibleObject> get(Object c) {
      Map<String, AccessibleObject> o = super.get(c);
      if (o == null) {
        o = new HashMap<String, AccessibleObject>();
        put((Class) c, o);
      }
      return o;
    }
  };
  private static Logger logger = Logger.getLogger(Mustache.class.getName());

  private static class Nothing extends AccessibleObject {
  }

  private static Nothing nothing = new Nothing();

  @Override
  public Object handleObject(Object scope, String name) {
    if (scope == null) return null;
    if (scope instanceof Future) {
      try {
        scope = ((Future) scope).get();
      } catch (Exception e) {
        throw new RuntimeException("Failed to get value from future", e);
      }
    }
    Object value = null;
    if (scope instanceof Map) {
      return ((Map) scope).get(name);
    }
    Class aClass = scope.getClass();
    Map<String, AccessibleObject> members;
    // Don't overload methods in your contexts
    members = cache.get(aClass);
    AccessibleObject member;
    synchronized (members) {
      member = members.get(name);
    }
    if (member == nothing) return null;
    if (member == null) {
      try {
        member = getField(name, aClass);
        synchronized (members) {
          members.put(name, member);
        }
      } catch (NoSuchFieldException e) {
        // Not set
      }
    }
    if (member == null) {
      try {
        synchronized (members) {
          member = getMethod(name, aClass);
          members.put(name, member);
        }
      } catch (NoSuchMethodException e) {
        try {
          synchronized (members) {
            member = getMethod(name, aClass, List.class);
            members.put(name, member);
          }
        } catch (NoSuchMethodException e1) {
          String propertyname = name.substring(0,
                  1).toUpperCase() + (name.length() > 1 ? name.substring(1) : "");
          try {
            synchronized (members) {
              member = getMethod("get" + propertyname, aClass);
              members.put(name, member);
            }
          } catch (NoSuchMethodException e2) {
            try {
              synchronized (members) {
                member = getMethod("is" + propertyname, aClass);
                members.put(name, member);
              }
            } catch (NoSuchMethodException e3) {
              // Nothing to be done
            }
          }
        }
      }
    }
    try {
      if (member instanceof Field) {
        Field field = (Field) member;
        value = field.get(scope);
        if (value == null) {
          if (field.getType().isAssignableFrom(Iterable.class)) {
            value = EMPTY;
          } else {
            value = NULL;
          }
        }
      } else if (member instanceof Method) {
        Method method = (Method) member;
        if (method.getParameterTypes().length == 0) {
          value = method.invoke(scope);
        }
        if (value == null) {
          if (method.getReturnType().isAssignableFrom(Iterable.class)) {
            value = EMPTY;
          } else {
            value = NULL;
          }
        }
      }
    } catch (Exception e) {
      // Might be nice for debugging but annoying in practice
      logger.log(Level.WARNING, "Failed to get value for " + name, e);
    }
    if (member == null) {
      synchronized (members) {
        members.put(name, nothing);
      }
    }
    return value;
  }

  @Override
  public Iterator iterate(Object object) {
    Iterator i;
    if (object instanceof Iterator) {
      return (Iterator) object;
    } else if (object instanceof Iterable) {
      i = ((Iterable) object).iterator();
    } else {
      i = new SingleValueIterator(object);
    }
    return i;
  }

  public static Method getMethod(String name, Class aClass, Class... params) throws NoSuchMethodException {
    Method member;
    try {
      member = aClass.getDeclaredMethod(name, params);
    } catch (NoSuchMethodException nsme) {
      Class superclass = aClass.getSuperclass();
      if (superclass != Object.class) {
        return getMethod(name, superclass, params);
      }
      throw nsme;
    }
    if ((member.getModifiers() & Modifier.PRIVATE) == Modifier.PRIVATE) {
      throw new NoSuchMethodException("Only public, protected and package methods allowed");
    }
    member.setAccessible(true);
    return member;
  }

  public static Field getField(String name, Class aClass) throws NoSuchFieldException {
    Field member;
    try {
      member = aClass.getDeclaredField(name);
    } catch (NoSuchFieldException nsfe) {
      Class superclass = aClass.getSuperclass();
      if (superclass != Object.class) {
        return getField(name, superclass);
      }
      throw nsfe;
    }
    if ((member.getModifiers() & Modifier.PRIVATE) == Modifier.PRIVATE) {
      throw new NoSuchFieldException("Only public, protected and package fields allowed");
    }
    member.setAccessible(true);
    return member;
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
