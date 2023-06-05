package com.github.mustachejava.reflect;

import com.github.mustachejava.Iteration;
import com.github.mustachejava.ObjectHandler;
import com.github.mustachejava.util.Wrapper;
import java.io.Writer;
import java.lang.reflect.Array;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;


abstract class AbstractObjectHandler implements ObjectHandler {

  protected static final Object NOT_FOUND = new Object();

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

  @Override
  public String stringify(Object object) {
    return object.toString();
  }

}
