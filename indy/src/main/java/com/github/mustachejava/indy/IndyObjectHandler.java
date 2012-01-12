package com.github.mustachejava.indy;

import java.lang.reflect.AccessibleObject;

import com.github.mustachejava.reflect.ReflectionObjectHandler;
import com.github.mustachejava.reflect.ReflectionWrapper;
import com.github.mustachejava.util.Wrapper;

/**
 * Creates custom classes instead of using reflection for handling objects. Leverages
 * the ReflectionObjectHandler to create the original wrappers and converts them to
 * new versions.
 */
public class IndyObjectHandler extends ReflectionObjectHandler {

  @Override
  public Wrapper find(String name, Object[] scopes) {
    ReflectionWrapper wrapper = (ReflectionWrapper) find(name, scopes);
    return wrapper;
  }

  @Override
  public Object coerce(Object object) {
    return super.coerce(object);
  }

  @Override
  protected Wrapper createWrapper(int scopeIndex, Wrapper[] wrappers, Class[] guard, AccessibleObject member, Object[] arguments) {
    return super.createWrapper(scopeIndex, wrappers, guard, member, arguments);
  }
}
