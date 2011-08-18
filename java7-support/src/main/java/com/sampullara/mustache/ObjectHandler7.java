package com.sampullara.mustache;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Implementation of Handle Object for Java VMs that include MethodHandles.
 * <p/>
 * User: sam
 * Date: 7/24/11
 * Time: 2:56 PM
 */
public class ObjectHandler7 implements ObjectHandler {

  private static Logger logger = Logger.getLogger(Mustache.class.getName());

  private static ClassValue<Map<String, MethodHandle>> cache = new ClassValue<Map<String, MethodHandle>>() {
    @Override
    protected Map<String, MethodHandle> computeValue(Class<?> type) {
      return new ConcurrentHashMap<>();
    }
  };

  private static final MethodHandles.Lookup LOOKUP = MethodHandles.lookup();

  // Need something to stand for unable to find field or method
  private static final MethodHandle NOHANDLE =
      MethodHandles.dropArguments(
          MethodHandles.constant(Object.class, Object.class),
          0, Object.class);

  public Object handleObject(Object parent, Scope scope, String name) {
    Object value = null;
    Class aClass = parent.getClass();
    Map<String, MethodHandle> handleMap = cache.get(aClass);
    MethodHandle handle = handleMap.get(name);
    if (handle == NOHANDLE) return null;
    try {
      if (handle == null) {
        try {
          Field field = getField(name, aClass);
          handleMap.put(name, handle = MethodHandles.lookup().unreflectGetter(field));
        } catch (IllegalAccessException | NoSuchFieldException e) {
          // Not set
        }
      }
      if (handle == null) {
        try {
          Method method = getMethod(name, aClass);
          handleMap.put(name, handle = LOOKUP.unreflect(method));
        } catch (IllegalAccessException | NoSuchMethodException e) {
          try {
            Method method = getMethod(name, aClass, Scope.class);
            handleMap.put(name, handle = LOOKUP.unreflect(method));
          } catch (IllegalAccessException | NoSuchMethodException e1) {
            String propertyname = name.substring(0, 1).toUpperCase() + (name.length() > 1 ? name.substring(1) : "");
            try {
              Method method = getMethod("get" + propertyname, aClass);
              handleMap.put(name, handle = LOOKUP.unreflect(method));
            } catch (IllegalAccessException | NoSuchMethodException e2) {
              try {
                Method method = getMethod("is" + propertyname, aClass);
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
          value = handle.invoke(parent);
        } else {
          value = handle.invoke(parent, scope);
        }
      }
      if (value == null) {
        if (handle != null && handle.type().returnType().isAssignableFrom(Iterable.class)) {
          value = Scope.EMPTY;
        } else {
          value = Scope.NULL;
        }
      }
    } catch (Throwable e) {
      // Might be nice for debugging but annoying in practice
      logger.log(Level.WARNING, "Failed to get value for " + name, e);
    }
    if (handle == null) {
      handleMap.put(name, NOHANDLE);
    }
    return value;
  }

  private static Method getMethod(String name, Class aClass, Class... params) throws NoSuchMethodException {
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
    method.setAccessible(true);
    return method;
  }

  private static Field getField(String name, Class aClass) throws NoSuchFieldException {
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
    field.setAccessible(true);
    return field;
  }
}
