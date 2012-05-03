package com.github.mustachejava.codes;

import java.io.IOException;
import java.io.Writer;
import java.util.Arrays;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.logging.Logger;

import com.github.mustachejava.Code;
import com.github.mustachejava.Mustache;
import com.github.mustachejava.MustacheException;
import com.github.mustachejava.ObjectHandler;
import com.github.mustachejava.TemplateContext;
import com.github.mustachejava.reflect.MissingWrapper;
import com.github.mustachejava.util.GuardException;
import com.github.mustachejava.util.Wrapper;

/**
 * Simplest possible code implementaion with some default shared behavior
 */
public class DefaultCode implements Code {
  protected String appended;

  protected final ObjectHandler oh;
  protected final String name;
  protected final TemplateContext tc;
  protected final Mustache mustache;
  protected final String type;
  protected final boolean returnThis;

  // Debug callsites
  protected static boolean debug = Boolean.getBoolean("mustache.debug");
  protected static Logger logger = Logger.getLogger("mustache");


  public DefaultCode() {
    this(null, null, null, null, null);
  }

  public DefaultCode(TemplateContext tc, ObjectHandler oh, Mustache mustache, String name, String type) {
    this.oh = oh;
    this.mustache = mustache;
    this.type = type;
    this.name = name;
    this.tc = tc;
    returnThis = ".".equals(name);
  }

  public Code[] getCodes() {
    return mustache == null ? null : mustache.getCodes();
  }

  @Override
  public synchronized void init() {
    Code[] codes = getCodes();
    if (codes != null) {
      for (Code code : codes) {
        code.init();
      }
    }
  }

  public void setCodes(Code[] newcodes) {
    mustache.setCodes(newcodes);
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
  public Object get(Object[] scopes) {
    if (returnThis) {
      return scopes[scopes.length - 1];
    }
    // Loop over the wrappers and find the one that matches
    // this set of scopes or get a new one
    Wrapper[] wrappers = prevWrappers;
    if (wrappers != null) {
      for (Wrapper prevWrapper : wrappers) {
        try {
          return oh.coerce(prevWrapper.call(scopes));
        } catch (GuardException ge) {
          // Check the next one or create a new one
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
      if (debug) {
        // Ugly but generally not interesting
        if (!(this instanceof PartialCode)) {
          StringBuilder sb = new StringBuilder("Failed to find: ");
          sb.append(name).append(" (").append(tc.file()).append(":").append(tc.line()).append(
                  ") ").append("in");
          for (Object scope : scopes) {
            if (scope != null) {
              sb.append(" ").append(scope.getClass().getSimpleName());
            }
          }
          logger.warning(sb.toString());
        }
      }
    }
    return wrapper;
  }

  @Override
  public Writer execute(Writer writer, Object scope) {
    return execute(writer, new Object[]{scope});
  }

  /**
   * The default behavior is to run the codes and append the captured text.
   *
   * @param writer The writer to write the output to
   * @param scopes The scopes to evaluate the embedded names against.
   */
  @Override
  public Writer execute(Writer writer, Object[] scopes) {
    return appendText(runCodes(writer, scopes));
  }

  @Override
  public void identity(Writer writer) {
    try {
      if (name != null) {
        tag(writer, type);
        if (getCodes() != null) {
          runIdentity(writer);
          tag(writer, "/");
        }
      }
      appendText(writer);
    } catch (IOException e) {
      throw new MustacheException(e);
    }
  }

  protected void runIdentity(Writer writer) {
    int length = getCodes().length;
    for (int i = 0; i < length; i++) {
      getCodes()[i].identity(writer);
    }
  }

  private void tag(Writer writer, String tag) throws IOException {
    writer.write(tc.startChars());
    writer.write(tag);
    writer.write(name);
    writer.write(tc.endChars());
  }

  protected Writer appendText(Writer writer) {
    if (appended != null) {
      try {
        writer.write(appended);
      } catch (IOException e) {
        throw new MustacheException(e);
      }
    }
    return writer;
  }

  protected Writer runCodes(Writer writer, Object[] scopes) {
    Code[] codes = getCodes();
    if (codes != null) {
      for (Code code : codes) {
        writer = code.execute(writer, scopes);
      }
    }
    return writer;
  }

  @Override
  public void append(String text) {
    if (appended == null) {
      appended = text;
    } else {
      appended = appended + text;
    }
  }

  private ThreadLocal<Object[]> localScopes = new ThreadLocal<Object[]>();

  /**
   * Allocating new scopes is currently the only place where we are activtely allocating
   * memory within the templating system. It is possible that recycling these might lend
   * some additional benefit or using the same one in each thread. The only time this
   * grows is when there are recursive calls to the same scope. In most non-degenerate cases
   * we won't encounter that. Also, since we are copying the results across these boundaries
   * we don't have to worry about threads.
   */
  protected Object[] addScope(Object next, Object[] scopes) {
    Object[] iteratorScopes = scopes;
    if (next != null) {
      // Need to expand the scopes holder
      iteratorScopes = localScopes.get();
      if (iteratorScopes == null) {
        iteratorScopes = new Object[scopes.length + 1];
        localScopes.set(iteratorScopes);
      } else {
        if (iteratorScopes.length < scopes.length + 1) {
          // Need to expand the scopes holder
          iteratorScopes = new Object[scopes.length + 1];
          localScopes.set(iteratorScopes);
        }
      }
      int srcPos = iteratorScopes.length - scopes.length - 1;
      System.arraycopy(scopes, 0, iteratorScopes, srcPos, scopes.length);
      for (; srcPos > 0; srcPos--) {
        iteratorScopes[srcPos - 1] = null;
      }
      iteratorScopes[iteratorScopes.length - 1] = next;
    }
    return iteratorScopes;
  }
}
