package com.github.mustachejava.reflect;

import com.github.mustachejava.Binding;
import com.github.mustachejava.Code;
import com.github.mustachejava.MustacheException;
import com.github.mustachejava.ObjectHandler;
import com.github.mustachejava.TemplateContext;
import com.github.mustachejava.codes.DefaultCode;
import com.github.mustachejava.codes.PartialCode;
import com.github.mustachejava.reflect.MissingWrapper;
import com.github.mustachejava.util.GuardException;
import com.github.mustachejava.util.Wrapper;

import java.util.Arrays;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * Codes are bound to their variables through bindings.
 * <p/>
 * User: sam
 * Date: 7/7/12
 * Time: 6:05 PM
 */
public class GuardedBinding implements Binding {
  private final ObjectHandler oh;
  private final TemplateContext tc;
  private final String name;
  private final Code code;

  public GuardedBinding(ObjectHandler oh, String name, TemplateContext tc, Code code) {
    this.name = name;
    this.code = code;
    this.oh = oh;
    this.tc = tc;
  }

  /**
   * The chances of a new guard every time is very low. Instead we will
   * store previously used guards and try them all before creating a new one.
   */
  private Set<Wrapper> previousSet = new CopyOnWriteArraySet<Wrapper>();
  private volatile Wrapper[] prevWrappers;

  /**
   * Retrieve the first value in the stacks of scopes that matches
   * the give name. The method wrappers are cached and guarded against
   * the type or number of scopes changing.
   * <p/>
   * Methods will be found using the object handler, called here with
   * another lookup on a guard failure and finally coerced to a final
   * value based on the ObjectHandler you provide.
   *
   * @param scopes An array of scopes to interrogate from right to left.
   * @return The value of the field or method
   */
  @Override
  public Object get(Object[] scopes) {
    // Loop over the wrappers and find the one that matches
    // this set of scopes or get a new one
    Wrapper current = null;
    Wrapper[] wrappers = prevWrappers;
    if (wrappers != null) {
      for (Wrapper prevWrapper : wrappers) {
        try {
          current = prevWrapper;
          return oh.coerce(prevWrapper.call(scopes));
        } catch (GuardException ge) {
          // Check the next one or create a new one
        } catch (MustacheException me) {
          throw new MustacheException("Failed: " + current, me);
        }
      }
    }
    return createAndGet(scopes);
  }

  private Object createAndGet(Object[] scopes) {
    // Make a new wrapper for this set of scopes and add it to the set
    Wrapper wrapper = getWrapper(name, scopes);
    previousSet.add(wrapper);
    if (prevWrappers == null || prevWrappers.length != previousSet.size()) {
      prevWrappers = previousSet.toArray(new Wrapper[previousSet.size()]);
    }
    // If this fails the guard, there is a bug
    try {
      return oh.coerce(wrapper.call(scopes));
    } catch (GuardException e) {
      throw new AssertionError(
              "Unexpected guard failure: " + previousSet + " " + Arrays.asList(scopes));
    }
  }

  protected synchronized Wrapper getWrapper(String name, Object[] scopes) {
    Wrapper wrapper = oh.find(name, scopes);
    if (wrapper instanceof MissingWrapper) {
      if (DefaultCode.debug) {
        // Ugly but generally not interesting
        if (!(code instanceof PartialCode)) {
          StringBuilder sb = new StringBuilder("Failed to find: ")
                  .append(name)
                  .append(" (")
                  .append(tc.file())
                  .append(":")
                  .append(tc.line())
                  .append(") ")
                  .append("in");
          for (Object scope : scopes) {
            if (scope != null) {
              sb.append(" ").append(scope.getClass().getSimpleName());
            }
          }
          DefaultCode.logger.warning(sb.toString());
        }
      }
    }
    return wrapper;
  }
}
