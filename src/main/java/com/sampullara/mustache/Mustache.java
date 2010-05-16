package com.sampullara.mustache;

import com.sampullara.util.FutureWriter;

import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Base class for Mustaches.
 * <p/>
 * User: sam
 * Date: May 3, 2010
 * Time: 10:12:47 AM
 */
public abstract class Mustache {
  protected Logger logger = Logger.getLogger(getClass().getName());
  private File root;
  private String path;

  public void setRoot(File root) {
    this.root = root;
  }

  public void setPath(String path) {
    this.path = path;
  }

  public String getPath() {
    return path;
  }

  public abstract void execute(FutureWriter writer, Scope ctx) throws MustacheException;

  protected void enqueue(final FutureWriter writer, final Mustache m, final Scope s) {
    writer.enqueue(new Callable<Object>() {
      @Override
      public Object call() throws Exception {
        FutureWriter fw = new FutureWriter(writer.getWriter());
        m.execute(fw, s);
        return fw;
      }
    });
  }

  protected void write(Writer writer, Scope s, String name, boolean encode) throws MustacheException {
    Object value = getValue(s, name);
    if (value != null) {
      if (value instanceof Future) {
        try {
          value = ((Future)value).get();
        } catch (Exception e) {
          throw new MustacheException("Failed to evaluate future value: " + name, e);
        }
      }
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

  private static Iterable emptyIterable = new ArrayList(0);

  protected Iterable<Scope> iterable(final Scope s, final String name) {
    final Object value = getValue(s, name);
    if (value == null || (value instanceof Boolean && !((Boolean) value))) {
      return emptyIterable;
    }
    return new Iterable<Scope>() {
      public Iterator<Scope> iterator() {
        return new Iterator<Scope>() {
          Iterator i;

          {
            if (value instanceof Iterable) {
              i = ((Iterable) value).iterator();
            } else if (value instanceof Boolean) {
              i = Arrays.asList(true).iterator();
            } else {
              i = Arrays.asList(value).iterator();
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
    final Object value = s.get(name);
    boolean isntEmpty = value instanceof Iterable && ((Iterable) value).iterator().hasNext();
    if (isntEmpty || (value instanceof Boolean && ((Boolean) value))) {
      return emptyIterable;
    }
    Scope scope = new Scope(s);
    scope.put(name, true);
    return Arrays.asList(scope);
  }

  protected Object getValue(Scope s, String name) {
    try {
      return s.get(name);
    } catch (Exception e) {
      logger.warning("Failed: " + e + " using " + name);
    }
    return null;
  }

  private static Pattern findToEncode = Pattern.compile("&(?!\\w+;)|[\"<>\\\\]");

  protected static String encode(String value) {
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
