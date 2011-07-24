package com.sampullara.mustache;

import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * TODO: Edit this
 * <p/>
 * User: sam
 * Date: 7/24/11
 * Time: 3:02 PM
 */
public class ObjectHandler6 implements ObjectHandler {
  protected static Map<Class, Map<String, AccessibleObject>> cache = new ConcurrentHashMap<Class, Map<String, AccessibleObject>>();
  public static final Iterable EMPTY = new ArrayList(0);
  public static final Object NULL = new Object() { public String toString() { return ""; }};
  private static Logger logger = Logger.getLogger(Mustache.class.getName());

  private static class Nothing extends AccessibleObject {}
  private static Nothing nothing = new Nothing();

  @Override
  public Object handleObject(Object parent, Scope scope, String name) {
    Object value = null;
    Class aClass = parent.getClass();
    Map<String, AccessibleObject> members;
    synchronized (Mustache.class) {
      // Don't overload methods in your contexts
      members = cache.get(aClass);
      if (members == null) {
        members = new ConcurrentHashMap<String, AccessibleObject>();

        cache.put(aClass, members);
      }
    }
    AccessibleObject member = members.get(name);
    if (member == nothing) return null;
    if (member == null) {
      try {
        member = getField(name, aClass);
        member.setAccessible(true);
        members.put(name, member);
      } catch (NoSuchFieldException e) {
        // Not set
      }
    }
    if (member == null) {
      try {
        member = getMethod(name, aClass);
        member.setAccessible(true);
        members.put(name, member);
      } catch (NoSuchMethodException e) {
        try {
          member = getMethod(name, aClass, Scope.class);
          member.setAccessible(true);
          members.put(name, member);
        } catch (NoSuchMethodException e1) {
          String propertyname = name.substring(0, 1).toUpperCase() + (name.length() > 1 ? name.substring(1) : "");
          try {
            member = getMethod("get" + propertyname, aClass);
            member.setAccessible(true);
            members.put(name, member);
          } catch (NoSuchMethodException e2) {
            try {
              member = getMethod("is" + propertyname, aClass);
              member.setAccessible(true);
              members.put(name, member);
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
        value = field.get(parent);
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
          value = method.invoke(parent);
        } else {
          value = method.invoke(parent, scope);
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
      members.put(name, nothing);
    }
    return value;
  }

  private static AccessibleObject getMethod(String name, Class aClass, Class... params) throws NoSuchMethodException {
    AccessibleObject member;
    try {
      member = aClass.getDeclaredMethod(name, params);
    } catch (NoSuchMethodException nsme) {
      Class superclass = aClass.getSuperclass();
      if (superclass != Object.class) {
        return getMethod(name, superclass, params);
      }
      throw nsme;
    }
    return member;
  }

  private static AccessibleObject getField(String name, Class aClass) throws NoSuchFieldException {
    AccessibleObject member;
    try {
      member = aClass.getDeclaredField(name);
    } catch (NoSuchFieldException nsfe) {
      Class superclass = aClass.getSuperclass();
      if (superclass != Object.class) {
        return getField(name, superclass);
      }
      throw nsfe;
    }
    return member;
  }

}
