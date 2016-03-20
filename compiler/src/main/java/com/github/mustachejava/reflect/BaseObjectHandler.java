package com.github.mustachejava.reflect;

import com.github.mustachejava.Binding;
import com.github.mustachejava.Code;
import com.github.mustachejava.Iteration;
import com.github.mustachejava.ObjectHandler;
import com.github.mustachejava.TemplateContext;

import java.io.Writer;
import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;

public abstract class BaseObjectHandler implements ObjectHandler {
  @Override
  public Object coerce(Object object) {
    if (object instanceof Optional) {
      Optional optional = (Optional) object;
      if (optional.isPresent()) {
        return coerce(optional.get());
      } else {
        return null;
      }
    }
    return object;
  }

  @Override
  public Writer falsey(Iteration iteration, Writer writer, Object object, List<Object> scopes) {
    if (object != null) {
      if (object instanceof Boolean) {
        if ((Boolean) object) {
          return writer;
        }
      } else if (object instanceof String) {
        if (!object.toString().equals("")) {
          return writer;
        }
      } else if (object instanceof List) {
        List list = (List) object;
        int length = list.size();
        if (length > 0) return writer;
      } else if (object instanceof Iterable) {
        Iterable iterable = (Iterable) object;
        if (iterable.iterator().hasNext()) return writer;
      } else if (object instanceof Iterator) {
        Iterator iterator = (Iterator) object;
        if (iterator.hasNext()) return writer;
      } else if (object.getClass().isArray()) {
        int length = Array.getLength(object);
        if (length > 0) return writer;
      } else {
        // All other objects are truthy
        return writer;
      }
    }
    return iteration.next(writer, object, scopes);
  }

  @Override
  public abstract Binding createBinding(String name, TemplateContext tc, Code code);

  @SuppressWarnings("ForLoopReplaceableByForEach") // it allocates objects for foreach
  public Writer iterate(Iteration iteration, Writer writer, Object object, List<Object> scopes) {
    if (object == null) return writer;
    if (object instanceof Boolean) {
      if (!(Boolean) object) {
        return writer;
      }
    }
    if (object instanceof String) {
      if (object.toString().equals("")) {
        return writer;
      }
    }
    if (object instanceof List) {
      List list = (List) object;
      int length = list.size();
      for (int i = 0; i < length; i++) {
        writer = iteration.next(writer, coerce(list.get(i)), scopes);
      }
    } else if (object instanceof Iterable) {
      for (Object next : ((Iterable) object)) {
        writer = iteration.next(writer, coerce(next), scopes);
      }
    } else if (object instanceof Iterator) {
      Iterator iterator = (Iterator) object;
      while (iterator.hasNext()) {
        writer = iteration.next(writer, coerce(iterator.next()), scopes);
      }
    } else if (object.getClass().isArray()) {
      int length = Array.getLength(object);
      for (int i = 0; i < length; i++) {
        writer = iteration.next(writer, coerce(Array.get(object, i)), scopes);
      }
    } else {
      writer = iteration.next(writer, object, scopes);
    }
    return writer;
  }

  protected Field getField(Class aClass, String name) throws NoSuchFieldException {
    Field member;
    try {
      member = aClass.getDeclaredField(name);
    } catch (NoSuchFieldException nsfe) {
      Class superclass = aClass.getSuperclass();
      if (superclass != null && superclass != Object.class) {
        return getField(superclass, name);
      }
      throw nsfe;
    }
    checkField(member);
    member.setAccessible(true);
    return member;
  }

  protected Method getMethod(Class<?> aClass, String name, Class<?>... params) throws NoSuchMethodException {
    Method member;
    try {
      member = aClass.getMethod(name, params);
      if (member.getDeclaringClass() == Object.class) {
        throw new NoSuchMethodException();
      }
    } catch (NoSuchMethodException nsme) {
      try {
        member = aClass.getDeclaredMethod(name, params);
      } catch (NoSuchMethodException nsme2) {
        Class superclass = aClass.getSuperclass();
        if (superclass != null && superclass != Object.class) {
          return getMethod(superclass, name);
        }
        throw nsme2;
      }
    }
    checkMethod(member);
    member.setAccessible(true);
    return member;
  }

  protected AccessibleObject findMember(Class sClass, String name) {
    AccessibleObject ao;
    try {
      ao = getMethod(sClass, name);
    } catch (NoSuchMethodException e) {
      String propertyname = name.substring(0, 1).toUpperCase() +
              (name.length() > 1 ? name.substring(1) : "");
      try {
        ao = getMethod(sClass, "get" + propertyname);
      } catch (NoSuchMethodException e2) {
        try {
          ao = getMethod(sClass, "is" + propertyname);
        } catch (NoSuchMethodException e3) {
          try {
            ao = getField(sClass, name);
          } catch (NoSuchFieldException e4) {
            ao = null;
          }
        }
      }
    }
    return ao;
  }

  // We default to not allowing private methods
  protected void checkMethod(Method member) throws NoSuchMethodException {
    if ((member.getModifiers() & Modifier.PRIVATE) == Modifier.PRIVATE) {
      throw new NoSuchMethodException("Only public, protected and package members allowed");
    }
  }

  // We default to not allowing private fields
  protected void checkField(Field member) throws NoSuchFieldException {
    if ((member.getModifiers() & Modifier.PRIVATE) == Modifier.PRIVATE) {
      throw new NoSuchFieldException("Only public, protected and package members allowed");
    }
  }

  @Override
  public String stringify(Object object) {
    return object.toString();
  }
}
