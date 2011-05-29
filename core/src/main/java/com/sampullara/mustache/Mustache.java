package com.sampullara.mustache;

import com.google.common.base.Function;
import com.google.common.collect.Iterables;
import com.sampullara.util.FutureWriter;
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
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;
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
  private static final boolean debug = Boolean.getBoolean("mustache.debug");
  public static final boolean trace = Boolean.getBoolean("mustache.trace");

  private static final String IMPLICIT_CURRENT_ELEMENT_TOKEN = ".";

  private File root;
  private String path;
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

  public final void execute(Writer writer, Map map) throws MustacheException {
    execute(new FutureWriter(writer), new Scope(map));
  }

  public final void execute(Writer writer, JsonNode jsonNode) throws MustacheException {
    execute(new FutureWriter(writer), new Scope(jsonNode));
  }

  public final void execute(FutureWriter writer, Map map) throws MustacheException {
    execute(writer, new Scope(map));
  }

  public final void execute(FutureWriter writer, JsonNode jsonNode) throws MustacheException {
    execute(writer, new Scope(jsonNode));
  }

  private Code[] compiled;

  public void execute(FutureWriter writer, Scope ctx) throws MustacheException {
    for (Code code : compiled) {
      code.execute(writer, ctx);
    }
  }

  protected ThreadLocal<FutureWriter> capturedWriter = new ThreadLocal<FutureWriter>();
  protected ThreadLocal<FutureWriter> actual = new ThreadLocal<FutureWriter>();

  /**
   * Enqueue's a Mustache into the FutureWriter and starts evaluating it.
   *
   * @param writer
   * @param m
   * @param s
   * @throws IOException
   */
  protected void enqueue(FutureWriter writer, final Mustache m, final Scope s) throws IOException {
    if (capturedWriter.get() != null) {
      actual.set(writer);
      writer = capturedWriter.get();
    }
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
    if (capturedWriter.get() != null) {
      actual.set(writer);
      writer = capturedWriter.get();
    }
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
    MustacheTrace.Event event = null;
    if (trace) {
      Object parent = s.getParent();
      String traceName = parent == null ? s.getClass().getName() : parent.getClass().getName();
      event = MustacheTrace.addEvent("iterable: " + name, traceName);
    }
    final String finalName = name;
    final Object value = getValue(s, name);
    if (value instanceof Function) {
      if (trace) {
        event.end();
      }
      final Function f = (Function) value;
      return new Iterable<Scope>() {
        @Override
        public Iterator<Scope> iterator() {
          return new Iterator<Scope>() {
            boolean first = true;
            StringWriter writer = new StringWriter();

            @Override
            public synchronized boolean hasNext() {
              if (first) {
                capturedWriter.set(new FutureWriter(writer));
              } else {
                try {
                  capturedWriter.get().flush();
                  capturedWriter.set(null);
                  Object apply = f.apply(writer.toString());
                  actual.get().write(apply == null ? null : String.valueOf(apply));
                  actual.set(null);
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
                  iterable = ((Future) value).get();
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
            scope.put(finalName, value);
            return scope;
          }

          public void remove() {
            throw new UnsupportedOperationException();
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
      event = MustacheTrace.addEvent("compile partial: " + name, root.getName());
    }
    Mustache mustache;
    mustache = mj.parseFile(name + ".html");
    mustache.setMustacheJava(mj);
    mustache.setRoot(root);
    if (trace) {
      event.end();
    }
    return mustache;
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
    final Object value = getValue(s, name);
    if (trace) {
      event.end();
    }
    boolean isntEmpty = value instanceof Iterable && ((Iterable) value).iterator().hasNext();
    if (isntEmpty || (value instanceof Boolean && ((Boolean) value)) ||
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
        String location = name + " @ " + sb;
        if (!name.startsWith("_") && missing.put(location, true) == null) {
          final Object parent = s.getParent();
          logger.warning("No field, method or key found for: " + location + (parent == null ? "" : " with base scope parent: " + parent.getClass().getName()));
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
