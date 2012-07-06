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
public class ReflectionObjectHandler extends BaseObjectHandler {

  protected static final Method MAP_METHOD;

  public static Object unwrap(ObjectHandler oh, int scopeIndex, Wrapper[] wrappers, Object[] scopes) throws GuardException {
    Object scope = oh.coerce(scopes[scopeIndex]);
    // The value may be buried by . notation
    if (wrappers != null) {
      for (Wrapper wrapper : wrappers) {
        scope = oh.coerce(wrapper.call(new Object[]{scope}));
      }
    }
    return scope;
  }

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
    // Simple guard to break if the number of scopes at this call site have changed
    guards.add(new DepthGuard(length));
    NEXT:
    for (int i = length - 1; i >= 0; i--) {
      Object scope = scopes[i];
      if (scope == null) continue;
      // Make sure that the current scope is the same class
      guards.add(new ClassGuard(i, scope));
      List<Wrapper> wrappers = null;
      int dotIndex;
      String subname = name;
      // If there is dot notation, start evaluating it
      while ((dotIndex = subname.indexOf('.')) != -1) {
        final String lookup = subname.substring(0, dotIndex);
        subname = subname.substring(dotIndex + 1);
        // This is used for lookups but otherwise always succeeds
        guards.add(new DotGuard(lookup, i, scope));
        List<Predicate<Object[]>> wrapperGuard = new ArrayList<Predicate<Object[]>>(1);
        wrapper = findWrapper(0, null, wrapperGuard, scope, lookup);
        if (wrappers == null) wrappers = new ArrayList<Wrapper>();
        if (wrapper != null) {
          // We need to dig into a scope when dot notation shows up
          wrappers.add(wrapper);
          try {
            // Pull out the next level
            scope = coerce(wrapper.call(new Object[]{scope}));
          } catch (GuardException e) {
            throw new AssertionError(e);
          }
        } else {
          // Failed to find a wrapper for the next dot
          wrapperGuard.add(new ClassGuard(0, scope));
          guards.add(new WrappedGuard(this, i, wrappers, wrapperGuard));
          continue NEXT;
        }
        if (scope == null) {
          // Found a wrapper, but the result of was null
          wrapperGuard.add(new NullGuard());
          guards.add(new WrappedGuard(this, i, wrappers, wrapperGuard));
          // Break here to allow the wrapper to be returned with the partial evaluation of the dot notation
          break;
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
    return wrapper == null ? new MissingWrapper(guards.toArray(new Predicate[guards.size()])) : wrapper;
  }

  protected Wrapper findWrapper(final int scopeIndex, Wrapper[] wrappers, List<Predicate<Object[]>> guards, Object scope, final String name) {
    scope = coerce(scope);
    if (scope == null) return null;
    // If the scope is a map, then we use the get() method
    // to see if it contains a value named name.
    if (scope instanceof Map) {
      Map map = (Map) scope;
      if (!map.containsKey(name)) {
        guards.add(new MapGuard(this, scopeIndex, name, false, wrappers));
        return null;
      } else {
        guards.add(new MapGuard(this, scopeIndex, name, true, wrappers));
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

  protected Wrapper createWrapper(int scopeIndex, Wrapper[] wrappers, List<? extends Predicate<Object[]>> guard, AccessibleObject member, Object[] arguments) {
    //noinspection unchecked
    return new ReflectionWrapper(scopeIndex, wrappers, guard.toArray(new Predicate[guard.size()]), member, arguments, this);
  }

}
