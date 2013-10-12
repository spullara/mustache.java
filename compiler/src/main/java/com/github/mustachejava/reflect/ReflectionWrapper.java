package com.github.mustachejava.reflect;

import com.github.mustachejava.MustacheException;
import com.github.mustachejava.ObjectHandler;
import com.github.mustachejava.util.GuardException;
import com.github.mustachejava.util.Wrapper;

import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * Used for evaluating values at a callsite
 */
public class ReflectionWrapper extends GuardedWrapper {
  // Context
  protected final int scopeIndex;
  protected final Wrapper[] wrappers;
  protected final ObjectHandler oh;

  // Dispatch
  protected final Method method;
  protected final Field field;
  protected final Object[] arguments;

  public ReflectionWrapper(int scopeIndex, Wrapper[] wrappers, Guard[] guard, AccessibleObject method, Object[] arguments, ObjectHandler oh) {
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
    this(rw.scopeIndex, rw.wrappers, rw.guards, rw.method == null ? rw.field : rw.method, rw.arguments, rw.oh);
  }

  protected Object unwrap(Object[] scopes) {
    if (wrappers == null || wrappers.length == 0) {
      return scopes[scopeIndex];
    } else {
      return ReflectionObjectHandler.unwrap(oh, scopeIndex, wrappers, scopes);
    }
  }

  @Override
  public Object call(Object[] scopes) throws GuardException {
    guardCall(scopes);
    Object scope = oh.coerce(unwrap(scopes));
    try {
      if (scope == null) return null;
      if (method == null) {
        return field.get(scope);
      } else {
        return method.invoke(scope, arguments);
      }
    } catch (IllegalArgumentException e) {
      throw new MustacheException("Wrong method for object: " + method + " on " + scope, e);
    } catch (InvocationTargetException e) {
      throw new MustacheException("Failed to execute method: " + method, e.getTargetException());
    } catch (IllegalAccessException e) {
      throw new MustacheException("Failed to execute method: " + method, e);
    }
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

  public Wrapper[] getWrappers() {
    return wrappers;
  }
}
