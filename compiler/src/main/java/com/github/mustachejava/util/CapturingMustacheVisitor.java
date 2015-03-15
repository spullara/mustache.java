package com.github.mustachejava.util;

import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.DefaultMustacheVisitor;
import com.github.mustachejava.Mustache;
import com.github.mustachejava.TemplateContext;
import com.github.mustachejava.codes.IterableCode;
import com.github.mustachejava.codes.NotIterableCode;
import com.github.mustachejava.codes.ValueCode;

import java.io.Writer;
import java.util.List;

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
  public void value(TemplateContext tc, String variable, boolean encoded) {
    list.add(new ValueCode(tc, df, variable, encoded) {
      @Override
      public Object get(List<Object> scopes) {
        Object o = super.get(scopes);
        if (o != null) {
          captured.value(name, o.toString());
        }
        return o;
      }
    });
  }

  @Override
  public void iterable(TemplateContext tc, String variable, Mustache mustache) {
    list.add(new IterableCode(tc, df, mustache, variable) {

      @Override
      public Writer execute(Writer writer, List<Object> scopes) {
        Writer execute = super.execute(writer, scopes);
        captured.arrayEnd();
        return execute;
      }

      @Override
      public Writer next(Writer writer, Object next, List<Object> scopes) {
        captured.objectStart();
        Writer nextObject = super.next(writer, next, scopes);
        captured.objectEnd();
        return nextObject;
      }

      @Override
      public Object get(List<Object> scopes) {
        Object o = super.get(scopes);
        captured.arrayStart(name);
        return o;
      }
    });
  }

  @Override
  public void notIterable(TemplateContext tc, String variable, Mustache mustache) {
    list.add(new NotIterableCode(tc, df, mustache, variable) {
      boolean called;

      @Override
      public Writer next(Writer writer, Object object, List<Object> scopes) {
        called = true;
        return super.next(writer, object, scopes);
      }

      @Override
      public Writer execute(Writer writer, List<Object> scopes) {
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
