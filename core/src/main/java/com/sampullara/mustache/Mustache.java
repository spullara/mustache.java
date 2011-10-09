package com.sampullara.mustache;

import com.google.common.base.Function;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

import com.sampullara.util.FutureWriter;
import com.sampullara.util.TemplateFunction;
import org.codehaus.jackson.JsonNode;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Stack;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

  protected static Logger logger = Logger.getLogger(Mustache.class.getName());
  protected static final boolean debug = Boolean.getBoolean("mustache.debug");
  public static final boolean trace = Boolean.getBoolean("mustache.trace");
  public static final boolean profile = Boolean.getBoolean("mustache.profile");
  private static final String IMPLICIT_CURRENT_ELEMENT_TOKEN = ".";

  // Debug
  protected static final ThreadLocal<Integer> line = new ThreadLocal<Integer>();

  private File root;
  private String path;
  private Code[] compiled;
  protected MustacheJava mj;

  public void setRoot(File root) {
    this.root = root;
  }

  public File getRoot() {
    return root;
  }

  public void setPath(String path) {
    this.path = path;
  }

  public String getPath() {
    return path;
  }

  public Scope unparse(String text) throws MustacheException {
    AtomicInteger position = new AtomicInteger(0);
    Scope unparse = unparse(text, position);
    if (unparse == null) {
      int min = Math.min(position.get() + 50, position.get() + Math.max(0, text.length() - position.get()));
      throw new MustacheException("Failed to match template at " + path + ":" + line.get() + " with text " +
          text.substring(position.get(), min));
    }
    return unparse;
  }

  protected Scope unparse(String text, AtomicInteger position) throws MustacheException {
    Scope current = new Scope();
    for (int i = 0; i < compiled.length && current != null; i++) {
      if (debug) {
        line.set(compiled[i].getLine());
      }
      Code[] truncate = truncate(compiled, i + 1);
      current = compiled[i].unparse(current, text, position, truncate);
    }
    return current;
  }

  public static Code[] truncate(Code[] codes, int start) {
    if (codes.length <= 1) return new Code[0];
    Code[] next = new Code[codes.length - start];
    System.arraycopy(codes, start, next, 0, next.length);
    return next;
  }

  /**
   * Execute the Mustache using a Map as the backing data and write the result
   * to the provided writer.
   *
   * @param writer
   * @param map
   * @return
   * @throws MustacheException
   */
  public final void execute(Writer writer, Map map) throws MustacheException, IOException {
    FutureWriter fw = new FutureWriter(writer);
    execute(fw, new Scope(map));
    fw.flush();
  }

  /**
   * Execute the Mustache using a JsonNode as the backing data and write the
   * result to the provided writer.
   *
   * @param writer
   * @param jsonNode
   * @return
   * @throws MustacheException
   */
  public final void execute(Writer writer, JsonNode jsonNode) throws MustacheException, IOException {
    FutureWriter fw = new FutureWriter(writer);
    execute(fw, new Scope(jsonNode));
    fw.flush();
  }

  /**
   * Execute the Mustache using a scope as the backing data and write the
   * result to the provided writer.
   *
   * @param writer a writer to write to
   * @param ctx context of the execution
   * @throws MustacheException
   */
  public final void execute(Writer writer, Scope ctx) throws MustacheException, IOException {
    FutureWriter fw = new FutureWriter(writer);
    execute(fw, ctx);
    fw.flush();
  }

  /**
   * Execute the Mustache using a Map as the backing data.
   *
   * @param writer
   * @param map
   * @throws MustacheException
   */
  public final void execute(FutureWriter writer, Map map) throws MustacheException {
    execute(writer, new Scope(map));
  }

  /**
   * Execute the Mustache using a JsonNode as a the backing data.
   *
   * @param writer
   * @param jsonNode
   * @throws MustacheException
   */
  public final void execute(FutureWriter writer, JsonNode jsonNode) throws MustacheException {
    execute(writer, new Scope(jsonNode));
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
   * Enqueues a Mustache into the FutureWriter.
   *
   * @param writer
   * @param m
   * @param s
   * @throws IOException
   */
  protected void enqueue(FutureWriter writer, final Mustache m, final Scope s) throws IOException {
    writer = pushWriter(writer);
    writer.enqueue(new Callable<Object>() {
      @Override
      public Object call() throws Exception {
        FutureWriter fw = new FutureWriter();
        m.setRoot(getRoot());
        m.setPath(getPath());
        m.execute(fw, s);
        return fw;
      }
    });
  }

  protected FutureWriter pushWriter(FutureWriter writer) {
    if (capturedWriter.get().size() > 0) {
      actual.get().push(writer);
      writer = capturedWriter.get().peek();
    }
    return writer;
  }

  /**
   * Writes a named value from the scope with optional encoding.
   *
   * @param writer
   * @param s
   * @param name
   * @param encode
   * @throws MustacheException
   */
  protected void write(Writer writer, Scope s, String name, boolean encode) throws MustacheException {
    MustacheTrace.Event event = null;
    if (trace) {
      Object parent = s.getParent();
      String traceName = parent == null ? s.getClass().getName() : parent.getClass().getName();
      event = MustacheTrace.addEvent("get: " + name, traceName);
    }
    Object value = getValue(s, name);
    if (trace) {
      event.end();
    }
    if (value != null) {
      if (value instanceof Future) {
        try {
          if (writer instanceof FutureWriter) {
            FutureWriter fw = (FutureWriter) writer;
            fw.enqueue((Future<Object>) value);
            return;
          }
          value = ((Future) value).get();
        } catch (Exception e) {
          throw new MustacheException("Failed to evaluate future value: " + name, e);
        }
      }
      if (value instanceof FutureWriter) {
        if (writer instanceof FutureWriter) {
          FutureWriter fw = (FutureWriter) writer;
          final Object finalValue = value;
          try {
            fw.enqueue(new Callable<Object>() {
              @Override
              public Object call() throws Exception {
                return finalValue;
              }
            });
          } catch (IOException e) {
            throw new MustacheException("Failed to enqueue future writer", e);
          }
        }
      } else {
        String string = String.valueOf(value);
        if (encode) {
          string = encode(string);
        }
        try {
          writer.write(string);
        } catch (IOException e) {
          throw new MustacheException("Failed to write: " + e);
        }
      }
    }
  }

  public void setCompiled(List<Code> compiled) {
    this.compiled = new ArrayList<Code>(compiled).toArray(new Code[compiled.size()]);
  }

  private class SingleValueIterator implements Iterator {
    private boolean done;
    private Object value;

    SingleValueIterator(Object value) {
      this.value = value;
    }

    @Override
    public boolean hasNext() {
      return !done;
    }

    @Override
    public Object next() {
      if (!done) {
        done = true;
        return value;
      }
      throw new NoSuchElementException();
    }

    @Override
    public void remove() {
      done = true;
    }
  }

  protected Iterable<Scope> ifiterable(final Scope s, final String name) {
    return Iterables.limit(Iterables.transform(iterable(s, name), new Function<Scope, Scope>() {
      public Scope apply(Scope scope) {
        scope.remove(name);
        return scope;
      }
    }), 1);
  }

  protected void iterable(FutureWriter writer, final Scope s, final String name, final Class<? extends Mustache> sub) throws IOException {
    writer = pushWriter(writer);
    writer.enqueue(new Callable<Object>() {
      @Override
      public Object call() throws Exception {
        FutureWriter fw = new FutureWriter();
        for (Scope s1 : iterable(s, name)) {
          enqueue(fw, sub.newInstance(), s1);
        }
        return fw;
      }
    });
  }

  /**
   * Iterate over a named value. If there is only one value return a single value iterator.
   *
   * @param s
   * @param name
   * @return
   */
  protected Iterable<Scope> iterable(final Scope s, final String name) {
    if (s == IdentityScope.one) {
      return Lists.newArrayList(s);
    }
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
                  logger.log(Level.SEVERE, "Could not get iterable: " + name, e);
                  return false;
                }
              }
              if (iterable instanceof Iterable && !(iterable instanceof JsonNode && !(((JsonNode) iterable).isArray()))) {
                i = ((Iterable) iterable).iterator();
              } else {
                i = new SingleValueIterator(iterable);
              }
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

  // Memory leak if you abuse TemplateFunction
  private static Map<String, Mustache> templateFunctionCache = new ConcurrentHashMap<String, Mustache>();

  public Iterable<Scope> function(final Scope scope, final Function f) {
    final boolean templateFunction = f instanceof TemplateFunction;
    final Scope s;
    if (templateFunction) {
      s = IdentityScope.one;
    } else {
      s = scope;
    }
    return new Iterable<Scope>() {
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
                      mustache = mj.parse(applyString);
                      templateFunctionCache.put(applyString, mustache);
                    }
                    mustache.execute(actual.get().pop(), scope);
                  }
                } else {
                  actual.get().pop().write(applyString);
                }
              } catch (Exception e) {
                logger.log(Level.SEVERE, "Could not apply function: " + f, e);
              }
            }
            return first;
          }

          @Override
          public synchronized Scope next() {
            if (first) {
              first = false;
              return s;
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

  public void setMustacheJava(MustacheJava mj) {
    this.mj = mj;
  }

  private Map<String, Mustache> partialCache = new ConcurrentHashMap<String, Mustache>();

  protected Mustache partial(String name) throws MustacheException {
    Mustache mustache = partialCache.get(name);
    if (mustache == null) {
      mustache = compilePartial(name);
      partialCache.put(name, mustache);
    }
    return mustache;
  }

  protected Mustache compilePartial(String name) throws MustacheException {
    MustacheTrace.Event event = null;
    if (trace) {
      event = MustacheTrace.addEvent("compile partial: " + name, root == null ? "classpath" : root.getName());
    }
    Mustache mustache;
    mustache = mj.parseFile(name + "." + getPartialExtension());
    mustache.setMustacheJava(mj);
    mustache.setRoot(root);
    if (trace) {
      event.end();
    }
    return mustache;
  }

  protected String getPartialExtension() {
    int index = path.lastIndexOf(".");
    return path.substring(index + 1);
  }

  protected void partial(FutureWriter writer, Scope s, final String name, Mustache partial) throws MustacheException {
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

  protected Iterable<Scope> inverted(final Scope s, final String name) {
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
        logger.log(Level.SEVERE, "Could not get inverted: " + name, e);
        return null;
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

  private static Map<String, Boolean> missing = new ConcurrentHashMap<String, Boolean>();

  protected Object getValue(Scope s, String name) {
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
            sb.append(path).append(":").append(ste.getLineNumber());
            break;
          }
        }
        if (sb.length() == 0) {
          sb.append(path).append(":").append(line.get());
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
      logger.warning("Failed: " + e + " using " + name);
    }
    return null;
  }

  private static Pattern findToEncode = Pattern.compile("&(?!\\w+;)|[\"<>\\\\\n]");

  // Override this in a super class if you don't want encoding.
  public String encode(String value) {
    StringBuffer sb = new StringBuffer();
    Matcher matcher = findToEncode.matcher(value);
    while (matcher.find()) {
      char c = matcher.group().charAt(0);
      switch (c) {
        case '&':
          matcher.appendReplacement(sb, "&amp;");
          break;
        case '\\':
          matcher.appendReplacement(sb, "\\\\");
          break;
        case '"':
          matcher.appendReplacement(sb, "&quot;");
          break;
        case '<':
          matcher.appendReplacement(sb, "&lt;");
          break;
        case '>':
          matcher.appendReplacement(sb, "&gt;");
          break;
        case '\n':
          matcher.appendReplacement(sb, "&#10;");
          break;
      }
    }
    matcher.appendTail(sb);
    return sb.toString();
  }
}
