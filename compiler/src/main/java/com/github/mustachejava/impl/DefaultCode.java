package com.github.mustachejava.impl;

import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.NoSuchElementException;

import com.github.mustachejava.Code;
import com.github.mustachejava.MustacheException;
import com.github.mustachejava.ObjectHandler;
import com.github.mustachejava.util.MethodGuardException;
import com.github.mustachejava.util.MethodWrapper;

/**
 * Simplest possible code implementaion with some default shared behavior
 */
public class DefaultCode implements Code {
  private StringBuilder sb = new StringBuilder();
  protected String appended;

  protected final ObjectHandler oh;
  protected final Code[] codes;
  protected final String name;
  protected final String type;
  protected final String sm;
  protected final String em;

  // Callsite caching
  protected MethodWrapper methodWrapper;

  // TODO: Recursion protection. Need better guard logic. But still fast.
  protected boolean notfound = false;
  protected boolean returnThis = false;

  public DefaultCode() {
    this(null, null, null, null, null, null);
  }

  public DefaultCode(ObjectHandler oh, Code[] codes, String name, String type, String sm, String em) {
    this.oh = oh;
    this.codes = codes;
    this.type = type;
    this.name = name;
    this.sm = sm;
    this.em = em;
    if (".".equals(name)) {
      returnThis = true;
    }
  }

  /**
   * Retrieve the first value in the stacks of scopes that matches
   * the give name. The method wrapper is cached and guarded against
   * the type or number of scopes changing. We should deepen the guard.
   *
   * Methods will be found using the object handler, called here with
   * another lookup on a guard failure and finally coerced to a final
   * value based on the ObjectHandler you provide.
   *
   * @param name   The common name of the field or method
   * @param scopes An array of scopes to interrogate from right to left.
   * @return The value of the field or method
   */
  public Object get(String name, Object... scopes) {
    if (notfound) return null;
    if (returnThis) {
      return scopes[scopes.length - 1];
    }
    if (methodWrapper == null) {
      methodWrapper = oh.find(name, scopes);
      if (methodWrapper == null) {
        notfound = true;
        return null;
      }
    }
    try {
      return oh.coerce(methodWrapper.call(scopes));
    } catch (MethodGuardException e) {
      methodWrapper = null;
      return get(name, scopes);
    }
  }

  /**
   * The default behavior is to run the codes and append the captured text.
   *
   * @param writer The writer to write the output to
   * @param scopes The scopes to evaluate the embedded names against.
   */
  @Override
  public Writer execute(Writer writer, Object... scopes) {
    return appendText(runCodes(writer, scopes));
  }

  @Override
  public void identity(Writer writer) {
    try {
      if (name != null) {
        tag(writer, type);
        if (codes != null) {
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
    int length = codes.length;
    for (int i = 0; i < length; i++) {
      codes[i].identity(writer);
    }
  }

  private void tag(Writer writer, String tag) throws IOException {
    writer.write(sm);
    writer.write(tag);
    writer.write(name);
    writer.write(em);
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

  protected Writer runCodes(Writer writer, Object... scopes) {
    if (codes != null) {
      int length = codes.length;
      for (int i = 0; i < length; i++) {
        writer = codes[i].execute(writer, scopes);
      }
    }
    return writer;
  }

  @Override
  public void append(String text) {
    sb.append(text);
    appended = sb.toString();
  }

  public static ArrayList EMPTY = new ArrayList(0);

  protected Iterator iterate(Object object) {
    Iterator i;
    if (object instanceof Iterator) {
      return (Iterator) object;
    } else if (object instanceof Iterable) {
      i = ((Iterable) object).iterator();
    } else {
      if (object == null) return EMPTY.iterator();
      if (object instanceof Boolean) {
        if (!(Boolean) object) {
          return EMPTY.iterator();
        }
      }
      if (object instanceof String) {
        if (object.toString().equals("")) {
          return EMPTY.iterator();
        }
      }
      i = new SingleValueIterator(object);
    }
    return i;
  }

  protected static class SingleValueIterator implements Iterator {
    private boolean done;
    private Object value;

    public SingleValueIterator(Object value) {
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

}
