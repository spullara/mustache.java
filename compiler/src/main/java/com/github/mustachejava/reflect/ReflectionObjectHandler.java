package com.github.mustachejava.reflect;

import com.github.mustachejava.Binding;
import com.github.mustachejava.Code;
import com.github.mustachejava.ObjectHandler;
import com.github.mustachejava.TemplateContext;
import com.github.mustachejava.util.GuardException;
import com.github.mustachejava.util.Wrapper;
import com.google.common.base.Predicate;

import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Method;
import java.util.ArrayList;
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
  
  @SuppressWarnings("unchecked")
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
    return wrapper == null ? new MissingWrapper(guards.toArray(new Predicate[guards.size()])) : wrapper;
  }

  protected Wrapper findWrapper(final int scopeIndex, Wrapper[] wrappers, List<Predicate<Object[]>> guards, Object scope, final String name) {
    scope = coerce(scope);
    if (scope == null) return null;
    // If the scope is a map, then we use the get() method
    // to see if it contains a value named name.
    if (scope instanceof Map) {
      Map map = (Map) scope;
      if (map.containsKey(name)) {
        guards.add(new MapGuard(this, scopeIndex, name, true, wrappers));
        return createWrapper(scopeIndex, wrappers, guards, MAP_METHOD, new Object[]{name});
      } else {
        guards.add(new MapGuard(this, scopeIndex, name, false, wrappers));
        if (!areMethodsAccessible(map)) {
          return null;
        }
      }
    }
    AccessibleObject member = findMember(scope.getClass(), name);
    return member == null ? null : createWrapper(scopeIndex, wrappers, guards, member, null);
  }

  @SuppressWarnings("unchecked")
  protected Wrapper createWrapper(int scopeIndex, Wrapper[] wrappers, List<? extends Predicate<Object[]>> guard, AccessibleObject member, Object[] arguments) {
    return new ReflectionWrapper(scopeIndex, wrappers, guard.toArray(new Predicate[guard.size()]), member, arguments, this);
  }

  @Override
  public Binding createBinding(String name, TemplateContext tc, Code code) {
    return new GuardedBinding(this, name, tc, code);
  }

  protected boolean areMethodsAccessible(Map<?, ?> map) {
    return false;
  }

  
}
