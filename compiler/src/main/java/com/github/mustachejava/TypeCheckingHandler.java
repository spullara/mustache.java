package com.github.mustachejava;

import com.github.mustachejava.reflect.BaseObjectHandler;
import com.github.mustachejava.util.Wrapper;

import java.io.Writer;
import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;

/**
 * Rather than pulling values this looks only at types. To check if a template matches the shape
 * of your view classes, pass in the set of classes you expect to have at runtime in the scope.
 *
 * User: sam
 * Date: 2/3/13
 * Time: 9:43 AM
 */
public class TypeCheckingHandler extends BaseObjectHandler {

  @Override
  public Wrapper find(String name, List<Object> scopes) {
    for (Object scope : scopes) {
      if (!(scope instanceof Class)) {
        throw new MustacheException("Only classes allowed with this object handler: " + scope);
      }
    }
    int length = scopes.size();
    if (length == 0) {
      throw new MustacheException("Empty scopes");
    }
    for (int i = length - 1; i >= 0; i--) {
      Object scope = scopes.get(i);
      if (scope == null || !(scope instanceof Class)) {
        throw new MustacheException("Invalid scope: " + scope);
      }
      Class scopeClass = (Class) scope;
      final AccessibleObject member = findMember(scopeClass, name);
      if (member != null) {
        return scopes1 -> {
          if (member instanceof Field) {
            return ((Field) member).getType();
          } else if (member instanceof Method) {
            return ((Method) member).getReturnType();
          } else {
            throw new MustacheException("Member not a field or method: " + member);
          }
        };
      }
    }
    throw new MustacheException("Failed to find matching field or method: " + name + " in " + Arrays.asList(scopes));
  }

  @Override
  public Binding createBinding(final String name, TemplateContext tc, Code code) {
    return scopes -> find(name, scopes).call(scopes);
  }

  @Override
  public Writer falsey(Iteration iteration, Writer writer, Object object, List<Object> scopes) {
    // Iterate once in either case
    return iterate(iteration, writer, object, scopes);
  }

  @Override
  public Writer iterate(Iteration iteration, Writer writer, Object object, List<Object> scopes) {
    return iteration.next(writer, object, scopes);
  }

  @Override
  public String stringify(Object object) {
    if (object instanceof Class) {
      Class c = (Class) object;
      return "[" + c.getSimpleName() + "]";
    }
    throw new MustacheException("Object was not a class: " + object);
  }
}
