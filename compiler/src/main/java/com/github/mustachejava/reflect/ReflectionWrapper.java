package com.github.mustachejava.reflect;

import com.github.mustachejava.MustacheException;
import com.github.mustachejava.ObjectHandler;
import com.github.mustachejava.util.GuardException;
import com.github.mustachejava.util.Wrapper;

import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;

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

  protected Object unwrap(List<Object> scopes) {
    if (wrappers == null || wrappers.length == 0) {
      return scopes.get(scopeIndex);
    } else {
      return ReflectionObjectHandler.unwrap(oh, scopeIndex, wrappers, scopes);
    }
  }

  @Override
  public Object call(List<Object> scopes) throws GuardException {
    guardCall(scopes);
    Object scope = oh.coerce(unwrap(scopes));
    try {
      if (scope == null) return null;
      if (method == null) {
        return field.get(scope);
      } else {
        return method.invoke(scope, arguments);
      }
    } catch (IllegalArgumentException | IllegalAccessException e) {
      throw new MustacheException("Error accessing " + getTargetDescription() + " on " + elementToString(scope)
          + ", scope: [" + elementsToString(scopes, scopeIndex) + "]" + ", guards: " + Arrays.toString(guards), e);
    } catch (InvocationTargetException e) {
      throw new MustacheException("Error invoking " + getTargetDescription() + " on " + elementToString(scope), e.getTargetException());
    } catch (Exception e) {
      throw new MustacheException("Error invoking " + getTargetDescription() + " on " + elementToString(scope), e);
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

  private String getTargetDescription() {
    return method == null
        ? "field " + field.getDeclaringClass() + "." + field.getName()
        : "method " + method.getDeclaringClass().getCanonicalName() + "." + method.getName() + "(" + elementsToString(Arrays.asList(arguments), method.getParameterTypes().length - 1) + ")";
  }
  
  private String elementsToString(List<Object> objects, int showUpTo) {
    if (objects == null || objects.size() == 0 || showUpTo < 0) {
      return "";
    }
    StringBuilder sb = new StringBuilder();
    for (int i = 0; i <= showUpTo && i < objects.size(); i++) {
      if (sb.length() > 0)
        sb.append(",");
      sb.append(elementToString(objects.get(i)));
    }
    return sb.toString();
  }

  private String elementToString(Object object) {
    return object == null ? null : object.getClass().getCanonicalName() + '@' + object.hashCode();
  }
  
}
