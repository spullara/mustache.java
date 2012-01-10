package com.github.mustachejava.impl;

import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import com.github.mustachejava.MustacheException;

/**
 * Used for evaluating values at a callsite
 */
public class MethodWrapper {
  // Context
  private int scope;
  private MethodWrapper[] wrappers;

  // Dispatch
  private final Method method;
  private final Field field;
  private final Object[] arguments;
  private final Class guard;

  public MethodWrapper(Class guard, AccessibleObject method, Object... arguments) {
    if (method instanceof Field) {
      this.method = null;
      this.field = (Field) method;
    } else {
      this.method = (Method) method;
      this.field = null;
    }
    this.arguments = arguments;
    this.guard = guard;
  }

  public void setScope(int scope) {
    this.scope = scope;
  }

  public void addWrappers(MethodWrapper[] wrappers) {
    if (this.wrappers == null) {
      this.wrappers = wrappers;
    } else {
      MethodWrapper[] methodWrappers = new MethodWrapper[this.wrappers.length + wrappers.length];
      System.arraycopy(this.wrappers, 0, methodWrappers, 0, this.wrappers.length);
      System.arraycopy(wrappers, 0, methodWrappers, this.wrappers.length, wrappers.length);
      this.wrappers = methodWrappers;
    }
  }

  public Object call(Object... scopes) throws MethodGuardException {
    try {
      Object scope = scopes[this.scope];
      if (guard.isInstance(scope)) {
        // The value may be buried by . notation
        if (wrappers != null) {
          for (int i = 0; i < wrappers.length; i++) {
            scope = wrappers[i].call(scope);
          }
        }
        if (scope == null) return null;
        if (method == null) {
          return field.get(scope);
        } else {
          return method.invoke(scope, arguments);
        }
      }
      throw new MethodGuardException();
    } catch (InvocationTargetException e) {
      throw new MustacheException("Failed to execute method: " + method, e.getTargetException());
    } catch (IllegalAccessException e) {
      throw new MustacheException("Failed to execute method: " + method, e);
    }
  }
}
