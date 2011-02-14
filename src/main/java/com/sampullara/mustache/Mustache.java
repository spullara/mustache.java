package com.sampullara.mustache;

import com.google.common.base.Function;
import com.sampullara.util.FutureWriter;
import org.codehaus.jackson.JsonNode;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
public abstract class Mustache {
  protected static Logger logger = Logger.getLogger(Mustache.class.getName());
  private static final boolean debug = Boolean.getBoolean("mustache.debug");
  public static final boolean trace = Boolean.getBoolean("mustache.trace");
  private File root;
  private String path;

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

  public final void execute(FutureWriter writer, Map map) throws MustacheException {
    execute(writer, new Scope(map));
  }

  public final void execute(FutureWriter writer, JsonNode jsonNode) throws MustacheException {
    execute(writer, new Scope(jsonNode));
  }

  public abstract void execute(FutureWriter writer, Scope ctx) throws MustacheException;

  private ThreadLocal<FutureWriter> capturedWriter = new ThreadLocal<FutureWriter>();
  private ThreadLocal<FutureWriter> actual = new ThreadLocal<FutureWriter>();

  public static class Trace {
    private static ThreadLocal<Trace> traceThreadLocal = new ThreadLocal<Trace>();
    private static Map<Long, Trace> traces = new ConcurrentHashMap<Long, Trace>();

    public static class Event {
      public long start = System.currentTimeMillis();
      public long end;
      public String thread;
      public String name;
      public String parameter;

      public Event(String name, String parameter, String unique) {
        this.name = name;
        this.parameter = parameter;
        this.thread = unique;
      }

      public String toString() {
        return start + ",\"" + end + ",\"" + name.replace("\"", "\\\"") + "\",\"" + parameter.replace("\"", "\\\"") + "\"";
      }

      public void end() {
        end = System.currentTimeMillis();
      }
    }

    private List<Event> events = new ArrayList<Event>();

    public synchronized static Event addEvent(String name, String parameter) {
      Trace trace = traceThreadLocal.get();
      Event event = new Event(name, parameter, Thread.currentThread().getName());
      if (trace == null) {
        System.out.println("Current trace not set");
      } else {
        trace.events.add(event);
      }
      return event;
    }

    public static void toASCII(Writer w, long uniqueid, int range) throws IOException {
      Trace trace = traces.get(uniqueid);
      if (trace == null) return;
      // Find min and max time
      long min = Long.MAX_VALUE;
      long max = 0;
      for (Event event : trace.events) {
        if (event.end > max) max = event.end;
        if (event.start < min) min = event.start;
      }
      double scale = range / ((double) max - min);
      Collections.sort(trace.events, new Comparator<Event>() {
        @Override
        public int compare(Event event, Event event1) {
          return (int) (event.start - event1.start);
        }
      });
      for (Event event : trace.events) {
        int during = (int) Math.round((event.end - event.start) * scale);
        int before = (int) Math.round((event.start - min) * scale);
        int after = (int) Math.round((max - event.end) * scale);
        int extra = 0;
        if (event.end == 0) {
          during = 0;
          after = 0;
          extra = range - before;
        }
        int total = before + during + after;
        if (total < 80) {
          during += 80 - total;
        }
        if (total > 80) {
          during -= total - 80;
        }
        if (during == 0) continue;
        for (int i = 0; i < before; i++) {
          w.write("-");
        }
        for (int i = 0; i < during; i++) {
          w.write("*");
        }
        for (int i = 0; i < after; i++) {
          w.write("-");
        }
        for (int i = 0; i < extra; i++) {
          w.write("x");
        }
        w.write(" ");
        w.write(event.name);
        w.write("\n");
      }
      w.write("Time: " + (max - min) + "ms Operations: " + trace.events.size() + "\n");
    }

    public synchronized static void setUniqueId(long unique) {
      Trace trace = traces.get(unique);
      if (trace == null) {
        trace = new Trace();
        traces.put(unique, trace);
      }
      traceThreadLocal.set(trace);
    }
  }

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
    Trace.Event event = null;
    if (trace) {
      Object parent = s.getParent();
      String traceName = parent == null ? s.getClass().getName() : parent.getClass().getName();
      event = Trace.addEvent("get: " + name, traceName);
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
    return limit(transform(iterable(s, name), new Function<Scope, Scope>() {
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
  private Iterable<Scope> iterable(final Scope s, final String name) {
    Trace.Event event = null;
    if (trace) {
      Object parent = s.getParent();
      String traceName = parent == null ? s.getClass().getName() : parent.getClass().getName();
      event = Trace.addEvent("iterable: " + name, traceName);
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
                  e.printStackTrace();
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
    final Object finalValue = value;
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
                  e.printStackTrace();
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
            Trace.Event event = null;
            if (trace) {
              Object parent = s.getParent();
              String traceName = parent == null ? s.getClass().getName() : parent.getClass().getName();
              event = Trace.addEvent("iterable next: " + name, traceName);
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

  protected void partial(FutureWriter writer, Scope s, final String name) throws MustacheException {
    if (name != null) {
      Trace.Event event = null;
      if (trace) {
        Object parent = s.getParent();
        String traceName = parent == null ? s.getClass().getName() : parent.getClass().getName();
        event = Trace.addEvent("partial: " + name, traceName);
      }
      Object parent = s.get(name);
      Scope scope = parent == null ? s : new Scope(parent, s);
      MustacheCompiler c = new MustacheCompiler(root);
      Mustache mustache = c.parseFile(name + ".html");
      mustache.execute(writer, scope);
      if (trace) {
        event.end();
      }
    }
  }

  protected Iterable<Scope> inverted(final Scope s, final String name) {
    Trace.Event event = null;
    if (trace) {
      Object parent = s.getParent();
      String traceName = parent == null ? s.getClass().getName() : parent.getClass().getName();
      event = Trace.addEvent("inverted: " + name, traceName);
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
    Scope scope = new Scope(s);
    scope.put(name, true);
    return Arrays.asList(scope);
  }

  private static Map<String, Boolean> missing = new ConcurrentHashMap<String, Boolean>();

  protected Object getValue(Scope s, String name) {
    try {
      Object o = s.get(name);
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

  private static Pattern findToEncode = Pattern.compile("&(?!\\w+;)|[\"<>\\\\]");

  public static String encode(String value) {
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
          matcher.appendReplacement(sb, "\"");
          break;
        case '<':
          matcher.appendReplacement(sb, "&lt;");
          break;
        case '>':
          matcher.appendReplacement(sb, "&gt;");
          break;
      }
    }
    matcher.appendTail(sb);
    return sb.toString();
  }
}
