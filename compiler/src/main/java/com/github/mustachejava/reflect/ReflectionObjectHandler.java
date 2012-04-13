package com.github.mustachejava.reflect;

import java.io.Writer;
import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

import com.google.common.base.Predicate;

import com.github.mustachejava.Iteration;
import com.github.mustachejava.ObjectHandler;
import com.github.mustachejava.util.GuardException;
import com.github.mustachejava.util.Wrapper;

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
        List<ClassGuard> classGuards = Arrays.asList(new ClassGuard(0, scope));
        wrapper = findWrapper(0, null, classGuards, scope, lookup);
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
        if (scope == null) return null;
      }
      Wrapper[] foundWrappers = wrappers == null ? null : wrappers.toArray(
              new Wrapper[wrappers.size()]);
      Wrapper foundWrapper = findWrapper(i, foundWrappers, guards, scope, subname);
      if (foundWrapper != null) {
        wrapper = foundWrapper;
        break;
      }
    }
    return wrapper;
  }

  @Override
  public Object coerce(Object object) {
    return object;
  }

  protected Wrapper findWrapper(int scopeIndex, Wrapper[] wrappers, List<? extends Predicate<Object[]>> guard, Object scope, String name) {
    if (scope == null) return null;
    if (scope instanceof Map) {
      Map map = (Map) scope;
      if (map.get(name) == null) {
        return null;
      } else {
        return createWrapper(scopeIndex, wrappers, guard, MAP_METHOD, new Object[]{name});
      }
    }
    Class aClass = scope.getClass();
    // Don't overload methods in your contexts
    Wrapper member = null;
    try {
      member = getField(scopeIndex, wrappers, guard, name, aClass);
    } catch (NoSuchFieldException e) {
      // Not set
    }
    if (member == null) {
      try {
        member = getMethod(scopeIndex, wrappers, guard, name, aClass);
      } catch (NoSuchMethodException e) {
        try {
          member = getMethod(scopeIndex, wrappers, guard, name, aClass, List.class);
        } catch (NoSuchMethodException e1) {
          String propertyname = name.substring(0, 1).toUpperCase() +
                  (name.length() > 1 ? name.substring(1) : "");
          try {
            member = getMethod(scopeIndex, wrappers, guard, "get" + propertyname, aClass);
          } catch (NoSuchMethodException e2) {
            try {
              member = getMethod(scopeIndex, wrappers, guard, "is" + propertyname, aClass);
            } catch (NoSuchMethodException e3) {
              // Nothing to be done
            }
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

  protected void checkField(Field member) throws NoSuchFieldException {
    if ((member.getModifiers() & Modifier.PRIVATE) == Modifier.PRIVATE) {
      throw new NoSuchFieldException("Only public, protected and package members allowed");
    }
  }

  protected Wrapper createWrapper(int scopeIndex, Wrapper[] wrappers, List<? extends Predicate<Object[]>> guard, AccessibleObject member, Object[] arguments) {
    return new ReflectionWrapper(scopeIndex, wrappers, guard, member, arguments);
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

  private static class DepthGuard implements Predicate<Object[]> {
    private final int length;

    public DepthGuard(int length) {
      this.length = length;
    }

    @Override
    public int hashCode() {
      return length;
    }

    @Override
    public boolean equals(Object o) {
      if (o instanceof DepthGuard) {
        DepthGuard depthGuard = (DepthGuard) o;
        return length == depthGuard.length;
      }
      return false;
    }

    @Override
    public boolean apply(@Nullable Object[] objects) {
      return objects != null && length == objects.length;
    }
  }

  private class DotGuard implements Predicate<Object[]> {

    private final String lookup;
    private final int scopeIndex;
    private final Class classGuard;

    public DotGuard(String lookup, int scopeIndex, Object classGuard) {
      this.lookup = lookup;
      this.scopeIndex = scopeIndex;
      this.classGuard = classGuard.getClass();
    }

    @Override
    public int hashCode() {
      return (lookup.hashCode() * 43 + scopeIndex) * 43 + classGuard.hashCode();
    }

    @Override
    public boolean equals(Object o) {
      if (o instanceof DotGuard) {
        DotGuard other = (DotGuard) o;
        return scopeIndex == other.scopeIndex && lookup.equals(other.lookup) && classGuard.equals(other.classGuard);
      }
      return false;
    }

    @Override
    public boolean apply(Object[] objects) {
      return true;
    }
  }
}
