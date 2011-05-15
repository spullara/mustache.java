package com.sampullara.mustache;

import org.codehaus.jackson.JsonNode;

import java.beans.BeanDescriptor;
import java.beans.BeanInfo;
import java.beans.Beans;
import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

/**
 * The scope of the executing Mustache can include an object and a map of strings.  Each scope can also have a
 * parent scope that is checked after nothing is found in the current scope.
 * <p/>
 * User: sam
 * Date: May 3, 2010
 * Time: 4:14:26 PM
 */
public class Scope extends HashMap {
  protected static Map<Class, Map<String, AccessibleObject>> cache = new ConcurrentHashMap<Class, Map<String, AccessibleObject>>();
  public static final Iterable EMPTY = new ArrayList(0);
  public static final Object NULL = new Object() { public String toString() { return ""; }};

  private Object parent;
  private Scope parentScope;
  private static Logger logger = Logger.getLogger(Mustache.class.getName());

  public Scope() {
  }

  public Scope(Object parent) {
    if (parent instanceof Scope) {
      this.parentScope = (Scope) parent;
    } else {
      this.parent = parent;
    }
  }

  public Scope(Scope parentScope) {
    this.parentScope = parentScope;
  }

  public Scope(Object parent, Scope parentScope) {
    this.parentScope = parentScope;
    this.parent = parent;
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
    Object value = null;
    Iterable<String> components = split(name, ".");
    Scope current = this;
    Scope currentScope = scope;
    for (String component : components) {
      value = current.localGet(currentScope, component);
      if (value == null || value == NULL) {
        return null;
      }
      currentScope = current;
      current = new Scope(value);
    }
    return value;
  }

  private Object localGet(Scope scope, String name) {
    Object v = super.get(name);
    if (v == null) {
      if (parent != null) {
        if (parent instanceof Future) {
          try {
            parent = ((Future) parent).get();
          } catch (Exception e) {
            throw new RuntimeException("Failed to get value from future");
          }
        }
        if (parent instanceof Map) {
          v = ((Map) parent).get(name);
        } else if (parent instanceof JsonNode) {
          v = handleJsonNode(name);
        } else {
          v = handleObject(scope, name, v);
        }
      }
    }
    if (v == null) {
      if (parentScope != null) {
        v = parentScope.get(name, scope);
      }
    }
    if (v == null) {
      // Might be nice for debugging but annoying in practice
      // logger.warning("No field, method or key found for: " + name);
    }
    return v;
  }

  private static class Nothing extends AccessibleObject {}
  private static Nothing nothing = new Nothing();

  private Object handleObject(Scope scope, String name, Object v) {
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
            }
          }
        }
      }
    }
    try {
      if (member instanceof Field) {
        Field field = (Field) member;
        v = field.get(parent);
        if (v == null) {
          if (field.getType().isAssignableFrom(Iterable.class)) {
            v = EMPTY;
          } else {
            v = NULL;
          }
        }
      } else if (member instanceof Method) {
        Method method = (Method) member;
        if (method.getParameterTypes().length == 0) {
          v = method.invoke(parent);
        } else {
          v = method.invoke(parent, scope);
        }
        if (v == null) {
          if (method.getReturnType().isAssignableFrom(Iterable.class)) {
            v = EMPTY;
          } else {
            v = NULL;
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
    return v;
  }

  private AccessibleObject getMethod(String name, Class aClass, Class... params) throws NoSuchMethodException {
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

  private AccessibleObject getField(String name, Class aClass) throws NoSuchFieldException {
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

  private Object handleJsonNode(String name) {
    Object v;
    JsonNode jsonNode = (JsonNode) parent;
    JsonNode result = jsonNode.get(name);
    if (result == null || result.isNull()) return null;
    if (result.isTextual()) {
      v = result.getTextValue();
    } else if (result.isBoolean()) {
      v = result.getBooleanValue();
    } else {
      v = result;
    }
    return v;
  }

  public Object getParent() {
    return parent;
  }

  private static Iterable<String> split(final String s, final String d) {
    return new Iterable<String>() {
      public Iterator<String> iterator() {
        return new Iterator<String>() {
          int length = s.length();
          int current = 0;

          public boolean hasNext() {
            return current < length;
          }

          public String next() {
            int start = current;
            int i = s.indexOf(d, start);
            if (i == -1) {
              current = length;
              return s.substring(start);
            } else {
              current = i + d.length();
              return s.substring(start, i);
            }
          }

          public void remove() {
          }
        };
      }
    };
  }

}
