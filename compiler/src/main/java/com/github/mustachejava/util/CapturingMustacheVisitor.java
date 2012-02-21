package com.github.mustachejava.util;

import java.io.File;
import java.io.Writer;
import java.util.List;

import com.github.mustachejava.Code;
import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.DefaultMustacheVisitor;
import com.github.mustachejava.Mustache;
import com.github.mustachejava.codes.IterableCode;
import com.github.mustachejava.codes.NotIterableCode;
import com.github.mustachejava.codes.ValueCode;

/**
 * Grab a map of values returned from calls
 */
public class CapturingMustacheVisitor extends DefaultMustacheVisitor {

  private final Captured captured;

  public interface Captured {
    void value(String name, String value);

    void arrayStart(String name);

    void arrayEnd();

    void objectStart();

    void objectEnd();
  }

  public CapturingMustacheVisitor(DefaultMustacheFactory cf, Captured captured) {
    super(cf);
    this.captured = captured;
  }

  @Override
  public void value(String variable, boolean encoded, int line, String sm, String em) {
    list.add(new ValueCode(cf, variable, sm, em, encoded, line) {
      @Override
      public Object get(String name, Object[] scopes) {
        Object o = super.get(name, scopes);
        if (o != null) {
          captured.value(name, o.toString());
        }
        return o;
      }
    });
  }

  @Override
  public void iterable(String variable, Mustache mustache, String file, int start, String sm, String em) {
    list.add(new IterableCode(cf, mustache, variable, sm, em, file) {

      @Override
      public Writer execute(Writer writer, Object[] scopes) {
        Writer execute = super.execute(writer, scopes);
        captured.arrayEnd();
        return execute;
      }

      @Override
      public Writer next(Writer writer, Object next, Object... scopes) {
        captured.objectStart();
        Writer nextObject = super.next(writer, next, scopes);
        captured.objectEnd();
        return nextObject;
      }

      @Override
      public Object get(String name, Object[] scopes) {
        Object o = super.get(name, scopes);
        captured.arrayStart(name);
        return o;
      }
    });
  }

  @Override
  public void notIterable(String variable, Mustache mustache, String file, int start, String sm, String em) {
    list.add(new NotIterableCode(cf, mustache, variable, sm, em) {
      String name;
      Object value;
      boolean called;

      @Override
      public Object get(String name, Object[] scopes) {
        this.name = name;
        return super.get(name, scopes);
      }

      @Override
      public Writer next(Writer writer, Object object, Object[] scopes) {
        called = true;
        value = object;
        return super.next(writer, object, scopes);
      }

      @Override
      public Writer execute(Writer writer, Object[] scopes) {
        Writer execute = super.execute(writer, scopes);
        if (called) {
          captured.arrayStart(name);
          captured.arrayEnd();
        }
        return execute;
      }
    });
  }
}
