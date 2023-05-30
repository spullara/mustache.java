package com.github.mustachejava.reflection;

import static org.junit.Assert.assertEquals;

import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.Mustache;
import com.github.mustachejava.reflect.ReflectionObjectHandler;
import java.io.StringReader;
import java.io.StringWriter;
import org.junit.Test;

public class ReflectionObjectHandlerTest {

  @Test
  public void testFieldsUsed() {
    String template = "{{field}}";
    Object scope = new Object() {
      public final String field = "value";
    };
    ReflectionObjectHandler reflectionObjectHandler = new ReflectionObjectHandler();
    DefaultMustacheFactory mf = new DefaultMustacheFactory();
    mf.setObjectHandler(reflectionObjectHandler);

    Mustache m = mf.compile(new StringReader(template), "template");
    StringWriter stringWriter = new StringWriter();
    m.execute(stringWriter, scope);

    assertEquals("value", stringWriter.toString());
  }

  @Test
  public void testMethodsUsed() {
    String template = "{{method}}";
    Object scope = new Object() {
      public String method() {
        return "value";
      }
    };
    ReflectionObjectHandler reflectionObjectHandler = new ReflectionObjectHandler();
    DefaultMustacheFactory mf = new DefaultMustacheFactory();
    mf.setObjectHandler(reflectionObjectHandler);

    Mustache m = mf.compile(new StringReader(template), "template");
    StringWriter stringWriter = new StringWriter();
    m.execute(stringWriter, scope);

    assertEquals("value", stringWriter.toString());
  }

}
