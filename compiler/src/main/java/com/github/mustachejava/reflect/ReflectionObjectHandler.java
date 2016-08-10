package com.github.mustachejava.reflect;

import static java.util.Collections.*;

import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.github.mustachejava.Binding;
import com.github.mustachejava.Code;
import com.github.mustachejava.ObjectHandler;
import com.github.mustachejava.TemplateContext;
import com.github.mustachejava.reflect.guards.ClassGuard;
import com.github.mustachejava.reflect.guards.DepthGuard;
import com.github.mustachejava.reflect.guards.DotGuard;
import com.github.mustachejava.reflect.guards.MapGuard;
import com.github.mustachejava.reflect.guards.NullGuard;
import com.github.mustachejava.reflect.guards.WrappedGuard;
import com.github.mustachejava.util.GuardException;
import com.github.mustachejava.util.Wrapper;

/**
 * Lookup objects using reflection and execute them the same way.
 *
 * User: sam
 * Date: 7/24/11
 * Time: 3:02 PM
 */
public class ReflectionObjectHandler extends BaseObjectHandler {

  protected static final Method MAP_METHOD;

  public static Object unwrap(ObjectHandler oh, int scopeIndex, Wrapper[] wrappers, List<Object> scopes) throws GuardException {
    Object scope = oh.coerce(scopes.get(scopeIndex));
    // The value may be buried by . notation
    if (wrappers != null) {
      for (Wrapper wrapper : wrappers) {
        scope = oh.coerce(wrapper.call(ObjectHandler.makeList(scope)));
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
  public Wrapper find(String name, final List<Object> scopes) {
    Wrapper wrapper = null;
    final int length = scopes.size();
    List<Guard> guards = new ArrayList<>(length);
    // Simple guard to break if the number of scopes at this call site have changed
    guards.add(createDepthGuard(length));
    NEXT:
    for (int i = length - 1; i >= 0; i--) {
      Object scope = scopes.get(i);
      if (scope == null) continue;
      // Make sure that the current scope is the same class
      guards.add(createClassGuard(i, scope));
      List<Wrapper> wrappers = null;
      int dotIndex;
      String subname = name;
      // Try and find a wrapper using the simple name
      wrapper = findWrapper(i, null, guards, scope, subname);
      if (wrapper != null) {
        break;
      }
      // If there is dot notation, start evaluating it
      while ((dotIndex = subname.indexOf('.')) != -1) {
        final String lookup = subname.substring(0, dotIndex);
        subname = subname.substring(dotIndex + 1);
        // This is used for lookups but otherwise always succeeds
        guards.add(createDotGuard(i, scope, lookup));
        List<Guard> wrapperGuard = new ArrayList<>(1);
        wrapperGuard.add(createClassGuard(0, scope));
        wrapper = findWrapper(0, null, wrapperGuard, scope, lookup);
        if (wrappers == null) wrappers = new ArrayList<>();
        if (wrapper != null) {
          // We need to dig into a scope when dot notation shows up
          wrappers.add(wrapper);
          try {
            // Pull out the next level
            scope = coerce(wrapper.call(ObjectHandler.makeList(scope)));
          } catch (GuardException e) {
            throw new AssertionError(e);
          }
        } else {
          // Failed to find a wrapper for the next dot
          guards.add(createWrappedGuard(i, wrappers, wrapperGuard));
          continue NEXT;
        }
        if (scope == null) {
          // Found a wrapper, but the result of was null
          guards.add(createWrappedGuard(i, wrappers, singletonList(createNullGuard())));
          // Break here to allow the wrapper to be returned with the partial evaluation of the dot notation
          break;
        }
      }
      if (wrappers != null) {
        guards.add(createWrappedGuard(i, wrappers, singletonList((Guard) createClassGuard(0, scope))));
      }
      Wrapper[] foundWrappers = wrappers == null ? null : wrappers.toArray(new Wrapper[wrappers.size()]);
      wrapper = findWrapper(i, foundWrappers, guards, scope, subname);
      if (wrapper == null) {
        // If we have found any wrappers we need to keep them rather than return a missing wrapper
        // otherwise it will continue you on to other scopes and break context precedence
        if (wrappers != null) {
          wrapper = createMissingWrapper(subname, guards);
          break;
        }
      } else {
        break;
      }
    }
    return wrapper == null ? createMissingWrapper(name, guards) : wrapper;
  }

  /**
   * Find a wrapper given the current context. If not found, return null.
   *
   * @param scopeIndex the index into the scope array
   * @param wrappers the current set of wrappers to get here
   * @param guards the list of guards used to find this
   * @param scope the current scope
   * @param name the name in the scope
   * @return null if not found, otherwise a wrapper for this scope and name
   */
  protected Wrapper findWrapper(final int scopeIndex, Wrapper[] wrappers, List<Guard> guards, Object scope, final String name) {
    scope = coerce(scope);
    if (scope == null) return null;
    // If the scope is a map, then we use the get() method
    // to see if it contains a value named name.
    if (scope instanceof Map) {
      Map map = (Map) scope;
      if (map.containsKey(name)) {
        guards.add(createMapGuard(scopeIndex, wrappers, name, true));
        return createWrapper(scopeIndex, wrappers, guards, MAP_METHOD, new Object[]{name});
      } else {
        guards.add(createMapGuard(scopeIndex, wrappers, name, false));
        if (!areMethodsAccessible(map)) {
          return null;
        }
      }
    }
    AccessibleObject member = findMember(scope.getClass(), name);
    return member == null ? null : createWrapper(scopeIndex, wrappers, guards, member, new Object[0]);
  }

  // Factories

  protected MissingWrapper createMissingWrapper(String name, List<Guard> guards) {
    return new MissingWrapper(name, guards.toArray(new Guard[guards.size()]));
  }

  protected DotGuard createDotGuard(int i, Object scope, String lookup) {
    return new DotGuard(lookup, i, scope);
  }

  protected WrappedGuard createWrappedGuard(int i, List<Wrapper> wrappers, List<Guard> wrapperGuard) {
    return new WrappedGuard(this, i, wrappers, wrapperGuard);
  }

  protected NullGuard createNullGuard() {
    return new NullGuard();
  }

  protected DepthGuard createDepthGuard(int length) {
    return new DepthGuard(length);
  }

  protected ClassGuard createClassGuard(int i, Object scope) {
    return new ClassGuard(i, scope);
  }

  protected MapGuard createMapGuard(int scopeIndex, Wrapper[] wrappers, String name, boolean contains) {
    return new MapGuard(this, scopeIndex, name, contains, wrappers);
  }

  @SuppressWarnings("unchecked")
  protected Wrapper createWrapper(int scopeIndex, Wrapper[] wrappers, List<? extends Guard> guard, AccessibleObject member, Object[] arguments) {
    return new ReflectionWrapper(scopeIndex, wrappers, guard.toArray(new Guard[guard.size()]), member, arguments, this);
  }

  @Override
  public Binding createBinding(String name, TemplateContext tc, Code code) {
    return new GuardedBinding(this, name, tc, code);
  }

  protected boolean areMethodsAccessible(Map<?, ?> map) {
    return false;
  }
}
