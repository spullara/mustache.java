package com.github.mustachejava.reflect;

import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import com.github.mustachejava.MustacheException;
import com.github.mustachejava.util.GuardException;
import com.github.mustachejava.util.Wrapper;

/**
 * Used for evaluating values at a callsite
 */
public class ReflectionWrapper implements Wrapper {
  // Context
  protected int scopeIndex;
  protected Wrapper[] wrappers;

  // Dispatch
  protected final Method method;
  protected final Field field;
  protected final Object[] arguments;
  protected final Class[] guard;

  public ReflectionWrapper(int scopeIndex, Wrapper[] wrappers, Class[] guard, AccessibleObject method, Object... arguments) {
    this.wrappers = wrappers;
    if (method instanceof Field) {
      this.method = null;
      this.field = (Field) method;
    } else {
      this.method = (Method) method;
      this.field = null;
    }
    this.arguments = arguments;
    this.guard = guard;
    this.scopeIndex = scopeIndex;
  }

  public void addWrappers(Wrapper[] addedWrappers) {
    if (this.wrappers == null) {
      this.wrappers = addedWrappers;
    } else {
      Wrapper[] newWrappers = new Wrapper[this.wrappers.length + addedWrappers.length];
      System.arraycopy(wrappers, 0, newWrappers, 0, wrappers.length);
      System.arraycopy(newWrappers, 0, newWrappers, wrappers.length, newWrappers.length);
      wrappers = newWrappers;
    }
  }

  @Override
  public Object call(Object... scopes) throws GuardException {
    try {
      guardCall(scopes);
      Object scope = scopes[this.scopeIndex];
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
    } catch (InvocationTargetException e) {
      throw new MustacheException("Failed to execute method: " + method, e.getTargetException());
    } catch (IllegalAccessException e) {
      throw new MustacheException("Failed to execute method: " + method, e);
    }
  }

  private void guardCall(Object[] scopes) throws GuardException {
    int length = scopes.length;
    if (guard.length != length) {
      throw new GuardException();
    }
    for (int j = 0; j < length; j++) {
      Class guardClass = guard[j];
      if (guardClass != null && !guardClass.isInstance(scopes[j])) {
        throw new GuardException();
      }
    }
  }
}
