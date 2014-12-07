package com.github.mustachejava;

import org.junit.Test;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.StringReader;
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class AbstractClassTest {
  static abstract class AbstractFoo {
      public abstract String getValue();
  }

  static class Foo extends AbstractFoo {
      @Override
      public String getValue() {
          return "I am Foo";
      }
  }

  static class Bar extends AbstractFoo {
      @Override
      public String getValue() {
          return "I am Bar";
      }
  }

  static class Container {
      public final AbstractFoo foo;
      public Container(final AbstractFoo foo) {
          this.foo = foo;
      }
  }

  @Test
  public void testAbstractClass() throws IOException {
      final List<Container> containers = new ArrayList<>();
      containers.add(new Container(new Foo()));
      containers.add(new Container(new Bar()));
      HashMap<String, Object> scopes = new HashMap<>();
      Writer writer = new OutputStreamWriter(System.out);
      MustacheFactory mf = new DefaultMustacheFactory();
      Mustache mustache = mf.compile(new StringReader("{{#containers}} {{foo.value}} {{/containers}}"), "example");
      scopes.put("containers", containers);
      mustache.execute(writer, scopes);
      writer.flush();
  }

  @Test
  public void testAbstractClassNoDots() throws IOException {
      final List<Container> containers = new ArrayList<>();
      containers.add(new Container(new Foo()));
      containers.add(new Container(new Bar()));
      HashMap<String, Object> scopes = new HashMap<>();
      Writer writer = new OutputStreamWriter(System.out);
      MustacheFactory mf = new DefaultMustacheFactory();
      Mustache mustache = mf.compile(new StringReader("{{#containers}} {{#foo}}{{value}}{{/foo}} {{/containers}}"), "example");
      scopes.put("containers", containers);
      mustache.execute(writer, scopes);
      writer.flush();
  }
}
