package com.sampullara.mustache;

import com.sampullara.util.FutureWriter;
import org.codehaus.jackson.JsonNode;

import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
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
public abstract class Mustache {
  protected static Logger logger = Logger.getLogger(Mustache.class.getName());
  private static final boolean debug = Boolean.getBoolean("mustache.debug");
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

  public abstract void execute(FutureWriter writer, Scope ctx) throws MustacheException;

  /**
   * Enqueue's a Mustache into the FutureWriter and starts evaluating it.
   *
   * @param writer
   * @param m
   * @param s
   * @throws IOException
   */
  protected void enqueue(final FutureWriter writer, final Mustache m, final Scope s) throws IOException {
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
    Object value = getValue(s, name);
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

  /**
   * Iterate over a named value. If there is only one value return a single value iterator.
   *
   * @param s
   * @param name
   * @return
   */
  protected Iterable<Scope> iterable(final Scope s, String name) {
    int times = 0;
    final String finalName = name;
    Object value = getValue(s, name);
    if (value instanceof Future) {
      try {
        value = ((Future) value).get();
      } catch (Exception e) {
        e.printStackTrace();
      }
    }
    if (value == null || (value instanceof Boolean && !((Boolean) value))) {
      return EMPTY;
    }
    final Object finalValue = value;
    return new Iterable<Scope>() {
      public Iterator<Scope> iterator() {
        return new Iterator<Scope>() {
          Iterator i;

          {
            if (finalValue instanceof Iterable && !(finalValue instanceof JsonNode && !(((JsonNode) finalValue).isArray()))) {
              i = ((Iterable) finalValue).iterator();
            } else {
              i = new SingleValueIterator(finalValue);
            }
          }

          public boolean hasNext() {
            return i.hasNext();
          }

          public Scope next() {
            Object value = i.next();
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
    MustacheCompiler c = new MustacheCompiler(root);
    if (name != null) {
      Object parent = s.get(name);
      Scope scope = parent == null ? s : new Scope(parent, s);
      Mustache mustache = c.parseFile(name + ".html");
      mustache.execute(writer, scope);
    }
  }

  protected Iterable<Scope> inverted(final Scope s, final String name) {
    final Object value = getValue(s, name);
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
