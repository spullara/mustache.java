package com.sampullara.mustache;

import org.codehaus.jackson.JsonNode;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
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

/**
 * The scope of the executing Mustache can include an object and a map of strings.  Each scope can also have a
 * parent scope that is checked after nothing is found in the current scope.
 * <p/>
 * User: sam
 * Date: May 3, 2010
 * Time: 4:14:26 PM
 */
public class Scope extends HashMap {
  protected static ClassValue<Map<String, MethodHandle>> cache = new ClassValue<Map<String, MethodHandle>>() {
    @Override
    protected Map<String, MethodHandle> computeValue(Class<?> type) {
      return new ConcurrentHashMap<>();
    }
  };

  public static final Iterable EMPTY = new ArrayList(0);
  public static final Object NULL = new Object() {
    public String toString() {
      return "";
    }
  };
  private static final MethodHandles.Lookup LOOKUP = MethodHandles.lookup();

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

  private static Object nothingField = new Object();
  private static MethodHandle nothing;

  static {
    try {
      nothing = MethodHandles.lookup().unreflectGetter(Scope.class.getDeclaredField("nothingField"));
    } catch (IllegalAccessException | NoSuchFieldException e) {
      e.printStackTrace();
      throw new AssertionError("Failed to find field", e);
    }
  }

  private Object handleObject(Scope scope, String name, Object v) {
    Class aClass = parent.getClass();
    Map<String, MethodHandle> handleMap = cache.get(aClass);
    MethodHandle handle = handleMap.get(name);
    if (handle == nothing) return null;
    AccessibleObject member;
    try {
      if (handle == null) {
        try {
          Field field = getField(name, aClass);
          field.setAccessible(true);
          handleMap.put(name, handle = MethodHandles.lookup().unreflectGetter(field));
        } catch (IllegalAccessException | NoSuchFieldException e) {
          // Not set
        }
      }
      if (handle == null) {
        try {
          Method method = getMethod(name, aClass);
          method.setAccessible(true);
          handleMap.put(name, handle = LOOKUP.unreflect(method));
        } catch (IllegalAccessException | NoSuchMethodException e) {
          try {
            Method method = getMethod(name, aClass, Scope.class);
            method.setAccessible(true);
            handleMap.put(name, handle = LOOKUP.unreflect(method));
          } catch (IllegalAccessException | NoSuchMethodException e1) {
            String propertyname = name.substring(0, 1).toUpperCase() + (name.length() > 1 ? name.substring(1) : "");
            try {
              Method method = getMethod("get" + propertyname, aClass);
              method.setAccessible(true);
              handleMap.put(name, handle = LOOKUP.unreflect(method));
            } catch (IllegalAccessException | NoSuchMethodException e2) {
              try {
                Method method = getMethod("is" + propertyname, aClass);
                method.setAccessible(true);
                handleMap.put(name, handle = LOOKUP.unreflect(method));
              } catch (IllegalAccessException | NoSuchMethodException e3) {
                // Not set
              }
            }
          }
        }
      }
      if (handle != null) {
        if (handle.type().parameterCount() == 1) {
          v = handle.invoke(parent);
        } else {
          v = handle.invoke(parent, scope);
        }
      }
    } catch (Throwable e) {
      e.printStackTrace();
      // Might be nice for debugging but annoying in practice
      logger.log(Level.WARNING, "Failed to get value for " + name, e);
    }
    if (handle == null) {
      handleMap.put(name, nothing);
    }
    return v;
  }

  private Method getMethod(String name, Class aClass, Class... params) throws NoSuchMethodException {
    Method method;
    try {
      method = aClass.getDeclaredMethod(name, params);
    } catch (NoSuchMethodException nsme) {
      Class superclass = aClass.getSuperclass();
      if (superclass != Object.class) {
        return getMethod(name, superclass, params);
      }
      throw nsme;
    }
    return method;
  }

  private Field getField(String name, Class aClass) throws NoSuchFieldException {
    Field field;
    try {
      field = aClass.getDeclaredField(name);
    } catch (NoSuchFieldException nsfe) {
      Class superclass = aClass.getSuperclass();
      if (superclass != Object.class) {
        return getField(name, superclass);
      }
      throw nsfe;
    }
    return field;
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
