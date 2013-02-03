package com.github.mustachejava;

import com.github.mustachejava.reflect.BaseObjectHandler;
import com.github.mustachejava.util.Wrapper;

import java.util.Arrays;

/**
 * Rather than pulling values this looks only at types.
 * <p/>
 * User: sam
 * Date: 2/3/13
 * Time: 9:43 AM
 */
public class TypeCheckingHandler extends BaseObjectHandler {
  /**
   * Find a value named "name" in the array of scopes in reverse order.
   *
   * @param name
   * @param scopes
   * @return
   */
  @Override
  public Wrapper find(String name, Object[] scopes) {
    for (Object scope : scopes) {
      if (!(scope instanceof Class)) {
        throw new MustacheException("Only classes allowed with this object handler: " + scope);
      }
    }
    int length = scopes.length;
    if (length == 0) {
      throw new MustacheException("Empty scopes");
    }
    for (int i = length - 1; i >= 0; i--) {
      Object scope = scopes[i];
      if (scope == null) {
        throw new MustacheException("Intermediate null scope");
      }

    }
    throw new MustacheException("Failed to find matching field or method: " + name + " in " + Arrays.asList(scopes));
  }

  @Override
  public Binding createBinding(String name, TemplateContext tc, Code code) {
    return new Binding() {
      @Override
      public Object get(Object[] scopes) {
        return null;
      }
    };
  }
}
