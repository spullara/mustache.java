package com.github.mustachejava.reflect;

import com.github.mustachejava.MustacheException;
import com.github.mustachejava.util.GuardException;
import com.github.mustachejava.util.Wrapper;

import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;

import com.google.common.base.Predicate;

/**
 * Used for evaluating values at a callsite
 */
public class ReflectionWrapper extends GuardedWrapper {
  // Context
  protected int scopeIndex;
  protected Wrapper[] wrappers;

  // Dispatch
  protected final Method method;
  protected final Field field;
  protected final Object[] arguments;

  public ReflectionWrapper(int scopeIndex, Wrapper[] wrappers, List<? extends Predicate<Object[]>> guard, AccessibleObject method, Object[] arguments) {
    super(guard);
    this.wrappers = wrappers;
    if (method instanceof Field) {
      this.method = null;
      this.field = (Field) method;
    } else {
      this.method = (Method) method;
      this.field = null;
    }
    this.arguments = arguments;
    this.scopeIndex = scopeIndex;
  }

  public ReflectionWrapper(ReflectionWrapper rw) {
    this(rw.scopeIndex, rw.wrappers, rw.guard, rw.method == null ? rw.field : rw.method, rw.arguments);
  }

  @Override
  public Object call(Object[] scopes) throws GuardException {
    try {
      guardCall(scopes);
      Object scope = unwrap(scopes);
      if (scope == null) return null;
      if (method == null) {
        return field.get(scope);
      } else {
        return method.invoke(scope, arguments);
      }
    } catch (InvocationTargetException e) {
      throw new MustacheException("Failed to execute method: " + method, e.getTargetException());
    } catch (IllegalAccessException e) {
      throw new MustacheException("Failed to execute method: " + method, e);
    }
  }

  protected Object unwrap(Object[] scopes) throws GuardException {
    Object scope = scopes[scopeIndex];
    // The value may be buried by . notation
    if (wrappers != null) {
      for (int i = 0; i < wrappers.length; i++) {
        scope = wrappers[i].call(new Object[] { scope });
      }
    }
    return scope;
  }

  public Method getMethod() {
    return method;
  }

  public Field getField() {
    return field;
  }

  public Object[] getArguments() {
    return arguments;
  }
}
