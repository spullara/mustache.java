package com.sampullara.mustache;

import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

/**
 * TODO: Edit this
 * <p/>
 * User: sam
 * Date: May 3, 2010
 * Time: 4:14:26 PM
 */
public class Scope extends HashMap {
  protected static Map<Class, Map<String, AccessibleObject>> cache = new ConcurrentHashMap<Class, Map<String, AccessibleObject>>();

  private Object parent;
  private Scope parentScope;
  private Logger logger;

  public Scope() {
  }

  public Scope(Object parent) {
    this.parent = parent;
    logger = Logger.getLogger(parent.getClass().getName());
  }

  public Scope(Scope parentScope) {
    this.parentScope = parentScope;
    logger = Logger.getLogger(getClass().getName());
  }

  public Scope(Object parent, Scope parentScope) {
    this.parentScope = parentScope;
    this.parent = parent;
    logger = Logger.getLogger(getClass().getName());
  }

  public Scope getParentScope() {
    return parentScope;
  }

  @Override
  public Object get(Object o) {
    return get(o, this);
  }

  public Object get(Object o, Scope scope) {
    String name = o.toString();
    Object v = super.get(o);
    if (v == null) {
      if (parent != null) {
        Class aClass = parent.getClass();
        Map<String, AccessibleObject> members;
        synchronized (Mustache.class) {
          // Don't overload methods in your contexts
          members = cache.get(aClass);
          if (members == null) {
            members = new ConcurrentHashMap<String, AccessibleObject>();
          }
        }
        AccessibleObject member = members.get(name);
        if (member == null) {
          try {
            member = aClass.getDeclaredField(name);
            member.setAccessible(true);
            members.put(name, member);
          } catch (NoSuchFieldException e) {
            // Not set
          }
        }
        if (member == null) {
          try {
            member = aClass.getDeclaredMethod(name);
            member.setAccessible(true);
            members.put(name, member);
          } catch (NoSuchMethodException e) {
            try {
              member = aClass.getDeclaredMethod(name, Scope.class);
              member.setAccessible(true);
              members.put(name, member);
            } catch (NoSuchMethodException e1) {
            }
          }
        }
        try {
          if (member instanceof Field) {
            v = ((Field) member).get(parent);
          } else if (member instanceof Method) {
            Method method = (Method) member;
            if (method.getParameterTypes().length == 0) {
              v = method.invoke(parent);
            } else {
              v = method.invoke(parent, scope);
            }
          }
        } catch (Exception e) {
          logger.warning("Failed to get value for " + name + ": " + e);
        }
      }
    }
    if (v == null) {
      if (parentScope != null) {
        v = parentScope.get(o, scope);
      }
    }
    if (v == null) {
      logger.warning("No field, method or key found for: " + name);
    }
    return v;
  }
}
