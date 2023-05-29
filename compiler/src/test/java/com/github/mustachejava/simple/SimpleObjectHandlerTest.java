package com.github.mustachejava.simple;

import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.Mustache;
import com.github.mustachejava.reflect.SimpleObjectHandler;
import java.io.StringReader;
import java.io.StringWriter;
import org.junit.Test;
import static org.junit.Assert.assertEquals;

public class SimpleObjectHandlerTest {

  @Test
  public void testFieldsUsed() {
    String template = "{{field}}";
    Object scope = new Object() {
      public final String field = "value";
    };
    SimpleObjectHandler simpleObjectHandler = new SimpleObjectHandler();
    DefaultMustacheFactory mf = new DefaultMustacheFactory();
    mf.setObjectHandler(simpleObjectHandler);

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
    SimpleObjectHandler simpleObjectHandler = new SimpleObjectHandler();
    DefaultMustacheFactory mf = new DefaultMustacheFactory();
    mf.setObjectHandler(simpleObjectHandler);

    Mustache m = mf.compile(new StringReader(template), "template");
    StringWriter stringWriter = new StringWriter();
    m.execute(stringWriter, scope);

    assertEquals("value", stringWriter.toString());
  }
}
