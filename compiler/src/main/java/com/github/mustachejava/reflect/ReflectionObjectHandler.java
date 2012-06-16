package com.github.mustachejava.reflect;

import com.github.mustachejava.Iteration;
import com.github.mustachejava.ObjectHandler;
import com.github.mustachejava.util.GuardException;
import com.github.mustachejava.util.Wrapper;
import com.google.common.base.Predicate;

import java.io.Writer;
import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Lookup objects using reflection and execute them the same way.
 * <p/>
 * User: sam
 * Date: 7/24/11
 * Time: 3:02 PM
 */
public class ReflectionObjectHandler implements ObjectHandler {

  protected static final Method MAP_METHOD;

  static {
    try {
      MAP_METHOD = Map.class.getMethod("get", Object.class);
    } catch (NoSuchMethodException e) {
      throw new AssertionError(e);
    }
  }

  @Override
  public Wrapper find(String name, final Object[] scopes) {
    Wrapper wrapper = null;
    final int length = scopes.length;
    List<Predicate<Object[]>> guards = new ArrayList<Predicate<Object[]>>(scopes.length);
    guards.add(new DepthGuard(length));
    NEXT:
    for (int i = length - 1; i >= 0; i--) {
      Object scope = scopes[i];
      if (scope == null) continue;
      Predicate<Object[]> guard = new ClassGuard(i, scope);
      guards.add(guard);
      List<Wrapper> wrappers = null;
      int dotIndex;
      String subname = name;
      while ((dotIndex = subname.indexOf('.')) != -1) {
        final String lookup = subname.substring(0, dotIndex);
        subname = subname.substring(dotIndex + 1);
        guards.add(new DotGuard(lookup, i, scope));
        List<Predicate<Object[]>> wrapperGuard = new ArrayList<Predicate<Object[]>>(1);
        wrapperGuard.add(new ClassGuard(0, scope));
        wrapper = findWrapper(0, null, wrapperGuard, scope, lookup);
        if (wrapper != null) {
          if (wrappers == null) wrappers = new ArrayList<Wrapper>();
          wrappers.add(wrapper);
          try {
            scope = wrapper.call(new Object[]{scope});
          } catch (GuardException e) {
            throw new AssertionError(e);
          }
        } else {
          continue NEXT;
        }
        if (scope == null) {
          // Failed to find next dot
          wrapper = null;
          break NEXT;
        }
      }
      Wrapper[] foundWrappers = wrappers == null ? null : wrappers.toArray(
              new Wrapper[wrappers.size()]);
      wrapper = findWrapper(i, foundWrappers, guards, scope, subname);
      if (wrapper != null) {
        break;
      }
    }
    //noinspection unchecked
    return wrapper;
  }

  @Override
  public Object coerce(Object object) {
    return object;
  }

  protected Wrapper findWrapper(final int scopeIndex, Wrapper[] wrappers, List<Predicate<Object[]>> guards, Object scope, final String name) {
    scope = coerce(scope);
    if (scope == null) return null;
    // If the scope is a map, then we use the get() method
    // to see if it contains a value named name.
    if (scope instanceof Map) {
      Map map = (Map) scope;
      if (!map.containsKey(name)) {
        guards.add(new MapGuard(scopeIndex, name, false, wrappers));
        return null;
      } else {
        guards.add(new MapGuard(scopeIndex, name, true, wrappers));
        return createWrapper(scopeIndex, wrappers, guards, MAP_METHOD, new Object[]{name});
      }
    }
    Class aClass = scope.getClass();
    // Variable resolution looks for:
    // 1) method named name
    // 2) method named getName
    // 3) method named isName
    // 4) field named name
    Wrapper member = null;
    try {
      member = getMethod(scopeIndex, wrappers, guards, name, aClass);
    } catch (NoSuchMethodException e) {
      String propertyname = name.substring(0, 1).toUpperCase() +
              (name.length() > 1 ? name.substring(1) : "");
      try {
        member = getMethod(scopeIndex, wrappers, guards, "get" + propertyname, aClass);
      } catch (NoSuchMethodException e2) {
        try {
          member = getMethod(scopeIndex, wrappers, guards, "is" + propertyname, aClass);
        } catch (NoSuchMethodException e3) {
          try {
            member = getField(scopeIndex, wrappers, guards, name, aClass);
          } catch (NoSuchFieldException e4) {
            // Not set
          }
        }
      }
    }
    return member;
  }

  protected Wrapper getMethod(int scopeIndex, Wrapper[] wrappers, List<? extends Predicate<Object[]>> guard, String name, Class aClass, Class... params) throws NoSuchMethodException {
    Method member;
    try {
      member = aClass.getDeclaredMethod(name, params);
    } catch (NoSuchMethodException nsme) {
      Class superclass = aClass.getSuperclass();
      if (superclass != null && superclass != Object.class) {
        return getMethod(scopeIndex, wrappers, guard, name, superclass, params);
      }
      throw nsme;
    }
    checkMethod(member);
    member.setAccessible(true);
    return createWrapper(scopeIndex, wrappers, guard, member, null);
  }

  // We default to not allowing private methods
  protected void checkMethod(Method member) throws NoSuchMethodException {
    if ((member.getModifiers() & Modifier.PRIVATE) == Modifier.PRIVATE) {
      throw new NoSuchMethodException("Only public, protected and package members allowed");
    }
  }

  protected Wrapper getField(int scopeIndex, Wrapper[] wrappers, List<? extends Predicate<Object[]>> guard, String name, Class aClass) throws NoSuchFieldException {
    Field member;
    try {
      member = aClass.getDeclaredField(name);
    } catch (NoSuchFieldException nsfe) {
      Class superclass = aClass.getSuperclass();
      if (superclass != null && superclass != Object.class) {
        return getField(scopeIndex, wrappers, guard, name, superclass);
      }
      throw nsfe;
    }
    checkField(member);
    member.setAccessible(true);
    return createWrapper(scopeIndex, wrappers, guard, member, null);
  }

  // We default to not allowing private fields
  protected void checkField(Field member) throws NoSuchFieldException {
    if ((member.getModifiers() & Modifier.PRIVATE) == Modifier.PRIVATE) {
      throw new NoSuchFieldException("Only public, protected and package members allowed");
    }
  }

  protected Wrapper createWrapper(int scopeIndex, Wrapper[] wrappers, List<? extends Predicate<Object[]>> guard, AccessibleObject member, Object[] arguments) {
    //noinspection unchecked
    return new ReflectionWrapper(scopeIndex, wrappers, guard.toArray(new Predicate[guard.size()]), member, arguments, this);
  }

  @Override
  public Writer falsey(Iteration iteration, Writer writer, Object object, Object[] scopes) {
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
      } else if (object instanceof Object[]) {
        Object[] array = (Object[]) object;
        int length = array.length;
        if (length > 0) return writer;
      } else {
        // All other objects are truthy
        return writer;
      }
    }
    return iteration.next(writer, object, scopes);
  }

  public Writer iterate(Iteration iteration, Writer writer, Object object, Object[] scopes) {
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
    if (object instanceof Iterable) {
      for (Object next : ((Iterable) object)) {
        writer = iteration.next(writer, coerce(next), scopes);
      }
    } else if (object instanceof Iterator) {
      Iterator iterator = (Iterator) object;
      while (iterator.hasNext()) {
        writer = iteration.next(writer, coerce(iterator.next()), scopes);
      }
    } else if (object instanceof Object[]) {
      Object[] array = (Object[]) object;
      for (Object o : array) {
        writer = iteration.next(writer, coerce(o), scopes);
      }
    } else {
      writer = iteration.next(writer, object, scopes);
    }
    return writer;
  }

}
