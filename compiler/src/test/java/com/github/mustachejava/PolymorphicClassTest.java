package com.github.mustachejava;

import static org.junit.Assert.*;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.HashMap;

import org.junit.Test;

public class PolymorphicClassTest {
  static class Value {
    public String getText() {
      return "ok";
    }
  }

  static class A {
    public Value getValue() {
      return new Value();
    }
  }

  static class B extends A {
    @Override
    public Value getValue() {
      return new Value();
    }
  }

  String compile(String template, Object model) {
    final StringWriter buffer = new StringWriter();
    factory.compile(template).execute(buffer, model);
    return buffer.toString();
  }

  DefaultMustacheFactory factory = new DefaultMustacheFactory() {
    public Reader getReader(String resourceName) {
      return new StringReader(resourceName);
    }
  };

  /**
   * Test for issue 97, java.lang.IllegalArgumentException: object is not an instance of declaring class
   */
  @Test
  public void testPolyClass() throws IOException {
    HashMap<String, Object> model = new HashMap<>();
    model.put("x", new B());
    assertEquals("ok", compile("{{x.value.text}}", model));
    model.put("x", new A());
    assertEquals("ok", compile("{{x.value.text}}", model));
    model.put("x", new B());
    assertEquals("ok", compile("{{x.value.text}}", model));
  }

}
