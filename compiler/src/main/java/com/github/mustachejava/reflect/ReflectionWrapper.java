package com.github.mustachejava.reflect;

import com.github.mustachejava.MustacheException;
import com.github.mustachejava.ObjectHandler;
import com.github.mustachejava.util.GuardException;
import com.github.mustachejava.util.Wrapper;
import com.google.common.base.Predicate;

import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * Used for evaluating values at a callsite
 */
public class ReflectionWrapper extends GuardedWrapper {
  // Context
  protected int scopeIndex;
  protected Wrapper[] wrappers;
  private final ObjectHandler oh;

  // Dispatch
  protected final Method method;
  protected final Field field;
  protected final Object[] arguments;

  public ReflectionWrapper(int scopeIndex, Wrapper[] wrappers, Predicate<Object[]>[] guard, AccessibleObject method, Object[] arguments, ObjectHandler oh) {
    super(guard);
    this.wrappers = wrappers;
    this.oh = oh;
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
    this(rw.scopeIndex, rw.wrappers, rw.guard, rw.method == null ? rw.field : rw.method, rw.arguments, rw.oh);
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
    Object scope = oh.coerce(scopes[scopeIndex]);
    // The value may be buried by . notation
    if (wrappers != null) {
      for (Wrapper wrapper : wrappers) {
        scope = oh.coerce(wrapper.call(new Object[]{scope}));
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

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    if (field == null) {
      sb.append(method.toString());
      if (arguments != null) {
        for (Object arg : arguments) {
          sb.append(",").append(arg);
        }
      }
    } else {
      sb.append(field);
    }
    return sb.toString();
  }
}
