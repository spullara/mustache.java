package com.github.mustachejava.mh;

import com.github.mustachejava.MustacheException;
import com.github.mustachejava.reflect.ReflectionWrapper;
import com.github.mustachejava.util.GuardException;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

public class MHWrapper extends ReflectionWrapper {

  private MethodHandle mh;

  MHWrapper(ReflectionWrapper rw) throws IllegalAccessException {
    super(rw);
    Method method = rw.getMethod();
    if (method == null) {
      Field field = rw.getField();
      mh = MethodHandles.lookup().unreflectGetter(field);
    } else {
      mh = MethodHandles.lookup().unreflect(method);
    }
  }

  @Override
  public Object call(Object[] scopes) throws GuardException {
    try {
      guardCall(scopes);
      Object scope = unwrap(scopes);
      if (scope == null) return null;
      if (arguments == null) {
        return mh.bindTo(scope).invokeWithArguments();
      } else {
        return mh.bindTo(scope).invokeExact(arguments);
      }
    } catch (Throwable e) {
      throw new MustacheException("Failed to execute method: " + method, e);
    }
  }
}
