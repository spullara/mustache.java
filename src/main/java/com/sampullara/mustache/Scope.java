package com.sampullara.mustache;

import org.codehaus.jackson.JsonNode;

import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
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
    logger = Logger.getLogger(getClass().getName());
  }

  public Scope(Object parent) {
    if (parent instanceof Scope) {
      this.parentScope = (Scope) parent;
      logger = Logger.getLogger(getClass().getName());
    } else {
      this.parent = parent;
      logger = Logger.getLogger(parent.getClass().getName());
    }
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
    Object value = null;
    String[] components = name.split("\\.");
    Scope current = this;
    Scope currentScope = scope;
    for (String component : components) {
      value = current.localGet(currentScope, component);
      if (value == null) {
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
            parent = ((Future)parent).get();
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
      logger.warning("No field, method or key found for: " + name);
    }
    return v;
  }

  private Object handleObject(Scope scope, String name, Object v) {
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
    return v;
  }

  private Object handleJsonNode(String name) {
    Object v;
    JsonNode jsonNode = (JsonNode) parent;
    JsonNode result = jsonNode.get(name);
    if (result != null && result.isTextual()) {
      v = result.getTextValue();
    } else if (result != null && result.isBoolean()) {
      v = result.getBooleanValue();
    } else {
      v = result;
    }
    return v;
  }
}
