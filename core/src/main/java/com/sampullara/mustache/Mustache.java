package com.sampullara.mustache;

import com.google.common.base.Function;
import com.google.common.collect.MapMaker;
import com.sampullara.util.FutureWriter;
import com.sampullara.util.TemplateFunction;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Stack;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

import static com.google.common.collect.Iterables.limit;
import static com.google.common.collect.Iterables.transform;
import static com.sampullara.mustache.Scope.EMPTY;
import static com.sampullara.mustache.Scope.NULL;

/**
 * Base class for Mustaches.
 * <p/>
 * User: sam
 * Date: May 3, 2010
 * Time: 10:12:47 AM
 */
public class Mustache {

  // The implicit element token
  private static final String IMPLICIT_CURRENT_ELEMENT_TOKEN = ".";

  // Logging
  public static Logger logger = Logger.getLogger(Mustache.class.getName());

  // Debug configuration variables

  // Line number tracker for code
  public static final ThreadLocal<Integer> line = new ThreadLocal<Integer>();

  // Report missing values
  public static final boolean debug = Boolean.getBoolean("mustache.debug");

  // Trace execution of code
  public static final boolean trace = Boolean.getBoolean("mustache.trace");

  // Profile the time it takes to execute code and construct a hotspots list
  public static final boolean profile = Boolean.getBoolean("mustache.profile");

  // The name of the template
  private String name;

  // The code of the template
  private Code[] compiled;

  // A reference to the generator of the template
  protected MustacheJava mj;

  /**
   * Given text that was created by or that matches the shape of a mustache
   * template, return the scope that can be used to recreate the text.
   *
   * @param text
   * @return
   * @throws MustacheException
   */
  public Scope unexecute(String text) throws MustacheException {
    AtomicInteger position = new AtomicInteger(0);
    Scope unexecuted = unexecute(text, position);
    if (unexecuted == null) {
      int min = Math.min(position.get() + 50,
              position.get() + Math.max(0, text.length() - position.get()));
      throw new MustacheException(
              "Failed to match template at " + name + ":" + line.get() + " with text " +
                      text.substring(position.get(), min));
    }
    return unexecuted;
  }

  protected Scope unexecute(String text, AtomicInteger position) throws MustacheException {
    Scope current = new Scope();
    for (int i = 0; i < compiled.length && current != null; i++) {
      if (debug) {
        line.set(compiled[i].getLine());
      }
      Code[] truncate = truncate(compiled, i + 1, new Code[0]);
      current = compiled[i].unexecute(current, text, position, truncate);
    }
    return current;
  }

  public static Code[] truncate(Code[] codes, int start, Code[] next) {
    if (codes.length <= 1) return next;
    Code[] truncate = new Code[codes.length - start];
    System.arraycopy(codes, start, truncate, 0, truncate.length);
    return truncate;
  }

  /**
   * Execute the Mustache using an object as the backing data and write the result
   * to the provided writer.
   *
   * @param writer
   * @param parent
   * @return
   * @throws MustacheException
   */
  public void execute(Writer writer, Object parent) throws MustacheException, IOException {
    FutureWriter fw = new FutureWriter(writer);
    execute(fw, new Scope(parent));
    fw.flush();
  }

  /**
   * Execute the Mustache using a scope as the backing data and write the
   * result to the provided writer.
   *
   * @param writer a writer to write to
   * @param ctx    context of the execution
   * @throws MustacheException
   */
  public void execute(Writer writer, Scope ctx) throws MustacheException, IOException {
    FutureWriter fw = new FutureWriter(writer);
    execute(fw, ctx);
    fw.flush();
  }

  /**
   * Execute the Mustache using an object as a the backing data.
   *
   * @param writer
   * @param parent
   * @throws MustacheException
   */
  public void execute(FutureWriter writer, Object parent) throws MustacheException {
    execute(writer, new Scope(parent));
  }

  /**
   * Execute the Mustache using the provided Scope as the backing data.
   *
   * @param writer
   * @param ctx
   * @throws MustacheException
   */
  public void execute(FutureWriter writer, Scope ctx) throws MustacheException {
    for (Code code : compiled) {
      if (debug) {
        line.set(code.getLine());
      }
      code.execute(writer, ctx);
    }
  }

  /**
   * Regenerate the original template.
   * 
   * @param writer
   * @throws MustacheException
   */
  public void identity(FutureWriter writer) throws MustacheException {
    for (Code code : compiled) {
      if (debug) {
        line.set(code.getLine());
      }
      code.identity(writer);
    }
  }

  protected ThreadLocal<Stack<FutureWriter>> capturedWriter = new ThreadLocal<Stack<FutureWriter>>() {
    @Override
    protected Stack<FutureWriter> initialValue() {
      return new Stack<FutureWriter>();
    }
  };
  protected ThreadLocal<Stack<FutureWriter>> actual = new ThreadLocal<Stack<FutureWriter>>() {
    @Override
    protected Stack<FutureWriter> initialValue() {
      return new Stack<FutureWriter>();
    }
  };

  /**
   * Used for capturing bodies during evaluation.
   * 
   * @param writer
   * @return
   */
  public FutureWriter pushWriter(FutureWriter writer) {
    Stack<FutureWriter> capturedStack = capturedWriter.get();
    if (capturedStack.size() > 0) {
      Stack<FutureWriter> actualStack = actual.get();
      actualStack.push(writer);
      writer = capturedStack.peek();
    }
    return writer;
  }

  public void setCompiled(List<Code> compiled) {
    this.compiled = new ArrayList<Code>(compiled).toArray(new Code[compiled.size()]);
  }

  public Code[] getCompiled() {
    return compiled;
  }

  public String getName() {
    return name;
  }

  public void setName(String filename) {
    this.name = filename;
  }

  /**
   * Evaluate an if iterable by returning at most 1 iteration.
   * 
   * @param s
   * @param name
   * @return
   */
  public Iterable<Scope> ifiterable(final Scope s, final String name) {
    return limit(transform(iterable(s, name), new Function<Scope, Scope>() {
      public Scope apply(Scope scope) {
        scope.remove(name);
        return scope;
      }
    }), 1);
  }

  /**
   * Iterate over a named value. If there is only one value return a single value iterator.
   *
   * @param s
   * @param name
   * @return
   */
  public Iterable<Scope> iterable(final Scope s, final String name) {
    MustacheTrace.Event event = null;
    if (trace) {
      Object parent = s.getParent();
      String traceName = parent == null ? s.getClass().getName() : parent.getClass().getName();
      event = MustacheTrace.addEvent("iterable: " + name, traceName);
    }
    final Object value = getValue(s, name);
    if (value instanceof Function) {
      if (trace) {
        event.end();
      }
      return function(s, (Function) value);
    }
    if (trace) {
      event.end();
    }
    if (value == null || (value instanceof Boolean && !((Boolean) value))) {
      return EMPTY;
    }
    return new Iterable<Scope>() {
      public Iterator<Scope> iterator() {
        return new Iterator<Scope>() {
          Iterator i;
          Object iterable = value;

          public boolean hasNext() {
            if (i == null) {
              if (iterable instanceof Future) {
                try {
                  iterable = ((Future) iterable).get();
                  if (iterable == null || (iterable instanceof Boolean && !((Boolean) iterable))) {
                    return false;
                  }
                } catch (Exception e) {
                  throw new RuntimeException(e);
                }
              }
              i = s.getObjectHandler().iterate(iterable);
            }
            return i.hasNext();
          }

          public Scope next() {
            MustacheTrace.Event event = null;
            if (trace) {
              Object parent = s.getParent();
              String traceName = parent == null ? s.getClass().getName() : parent.getClass().getName();
              event = MustacheTrace.addEvent("iterable next: " + name, traceName);
            }
            Object value = i.next();
            if (trace) {
              event.end();
            }
            Scope scope;
            if (!(value instanceof Boolean)) {
              scope = new Scope(value, s);
            } else {
              scope = new Scope(s);
            }
            scope.put(name, value);
            return scope;
          }

          public void remove() {
            throw new UnsupportedOperationException();
          }
        };
      }
    };
  }

  // Mustache template function cache size. For i18n should be > #i18n stanzas * languages
  private static final int SIZE = Integer.getInteger("mustache.cachesize", 100000);
  private static Map<String, Mustache> templateFunctionCache = new MapMaker().maximumSize(SIZE).makeMap();

  /**
   * Explicit function iteration.
   * 
   * @param scope
   * @param f
   * @return
   */
  public FunctionIterator function(final Scope scope, final Function f) {
    final boolean templateFunction = f instanceof TemplateFunction;
    return new FunctionIterator(f) {
      @Override
      public Iterator<Scope> iterator() {
        return new Iterator<Scope>() {
          boolean first = true;
          StringWriter writer = new StringWriter();

          @Override
          public synchronized boolean hasNext() {
            if (first) {
              capturedWriter.get().push(new FutureWriter(writer));
            } else {
              try {
                capturedWriter.get().pop().flush();
                Object apply = f.apply(writer.toString());
                String applyString = apply == null ? null : String.valueOf(apply);
                if (templateFunction) {
                  if (applyString != null) {
                    Mustache mustache = templateFunctionCache.get(applyString);
                    if (mustache == null) {
                      mustache = mj.parse(applyString, name);
                      templateFunctionCache.put(applyString, mustache);
                    }
                    mustache.execute(actual.get().pop(), scope);
                  }
                } else {
                  actual.get().pop().write(applyString);
                }
              } catch (Exception e) {
                throw new RuntimeException(e);
              }
            }
            return first;
          }

          @Override
          public synchronized Scope next() {
            if (first) {
              first = false;
              return scope;
            }
            throw new NoSuchElementException();
          }

          @Override
          public void remove() {
          }
        };
      }
    };
  }

  /**
   * Set the mustache creator.
   * 
   * @param mj
   */
  public void setMustacheJava(MustacheJava mj) {
    this.mj = mj;
  }

  /**
   * Compile a partial in the context of this mustache.
   * 
   * @param name
   * @return
   * @throws MustacheException
   */
  public Mustache partial(String name) throws MustacheException {
    return compilePartial(name);
  }

  protected Mustache compilePartial(String name) throws MustacheException {
    MustacheTrace.Event event = null;
    if (trace) {
      event = MustacheTrace.addEvent("compile partial: " + name, "");
    }
    Mustache mustache;
    mustache = mj.parseFile(name + "." + getPartialExtension());
    mustache.setMustacheJava(mj);
    if (trace) {
      event.end();
    }
    return mustache;
  }

  protected String getPartialExtension() {
    int index = name.lastIndexOf(".");
    return name.substring(index + 1);
  }

  /**
   * Execute a partial in the context of this mustache.
   *
   * @param writer
   * @param s
   * @param name
   * @param partial
   * @throws MustacheException
   */
  public void partial(FutureWriter writer, Scope s, final String name, Mustache partial) throws MustacheException {
    if (name != null) {
      MustacheTrace.Event event = null;
      if (trace) {
        Object parent = s.getParent();
        String traceName = parent == null ? s.getClass().getName() : parent.getClass().getName();
        event = MustacheTrace.addEvent("partial: " + name, traceName);
      }
      Object parent = s.get(name);
      Scope scope = parent == null ? s : new Scope(parent, s);
      partial.execute(writer, scope);
      if (trace) {
        event.end();
      }
    }
  }

  /**
   * Inversion
   *
   * @param s
   * @param name
   * @return
   */
  public Iterable<Scope> inverted(final Scope s, final String name) {
    MustacheTrace.Event event = null;
    if (trace) {
      Object parent = s.getParent();
      String traceName = parent == null ? s.getClass().getName() : parent.getClass().getName();
      event = MustacheTrace.addEvent("inverted: " + name, traceName);
    }
    Object possibleFuture = getValue(s, name);
    while (possibleFuture instanceof Future) {
      try {
        possibleFuture = ((Future) possibleFuture).get();
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    }
    final Object value = possibleFuture;
    if (trace) {
      event.end();
    }

    boolean isntEmpty = value instanceof Iterable && ((Iterable) value).iterator().hasNext();
    if (isntEmpty ||
            (value instanceof Boolean && ((Boolean) value)) ||
            (value != null && !(value instanceof Iterable) && !(value instanceof Boolean))) {
      return EMPTY;
    }
    final Scope scope = new Scope(s);
    scope.put(name, true);
    return new Iterable<Scope>() {

      @Override
      public Iterator<Scope> iterator() {
        return new Iterator<Scope>() {
          Scope value = scope;

          @Override
          public boolean hasNext() {
            return value != null;
          }

          @Override
          public Scope next() {
            Scope tmp = value;
            value = null;
            return tmp;
          }

          @Override
          public void remove() {
          }
        };
      }
    };
  }

  // Don't over report missing values
  private static Map<String, Boolean> missing = new ConcurrentHashMap<String, Boolean>();

  /**
   * Get a value from the scope.
   * 
   * @param s
   * @param name
   * @return
   */
  public Object getValue(Scope s, String name) {
    try {

      Object o;
      // TODO: If we get the implicit current element, we just grab the
      // first value from the current scope. This is somewhat dangerous
      // if used in the wrong scope or if some additional values have
      // been added to the scope. We might want to figure out a more robust
      // way to implement this. If you do not use "." it won't matter
      if (IMPLICIT_CURRENT_ELEMENT_TOKEN.equals(name)) {
        o = s.values().iterator().next();
      } else {
        o = s.get(name);
      }

      if (o == null && debug) {
        StringBuilder sb = new StringBuilder();
        for (StackTraceElement ste : Thread.currentThread().getStackTrace()) {
          String className = ste.getClassName();
          if (className.startsWith("com.sampullara.mustaches.Mustache")) {
            sb.append(this.name).append(":").append(ste.getLineNumber());
            break;
          }
        }
        if (sb.length() == 0) {
          sb.append(this.name).append(":").append(line.get());
        }
        String location = name + " @ " + sb;
        if (!name.startsWith("_") && missing.put(location, true) == null) {
          logger.warning("No field, method or key found for: " + location + " " + s);
        }
      }
      if (o == NULL) {
        return null;
      }
      return o;
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }
}
