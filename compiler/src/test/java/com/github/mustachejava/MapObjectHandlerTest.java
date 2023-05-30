package com.github.mustachejava;

import static org.junit.Assert.assertEquals;

import com.github.mustachejava.reflect.MapObjectHandler;
import java.io.StringReader;
import java.io.StringWriter;
import org.junit.Test;

public class MapObjectHandlerTest {

  @Test
  public void testFieldsIgnored() {
    String template = "{{field}}";
    Object scope = new Object() {
      public final String field = "value";
    };
    MapObjectHandler mapObjectHandler = new MapObjectHandler();
    DefaultMustacheFactory mf = new DefaultMustacheFactory();
    mf.setObjectHandler(mapObjectHandler);

    Mustache m = mf.compile(new StringReader(template), "template");
    StringWriter stringWriter = new StringWriter();
    m.execute(stringWriter, scope);

    assertEquals("", stringWriter.toString());
  }

  @Test
  public void testMethodsIgnored() {
    String template = "{{method}}";
    Object scope = new Object() {
      public String method() {
        return "value";
      }
    };
    MapObjectHandler mapObjectHandler = new MapObjectHandler();
    DefaultMustacheFactory mf = new DefaultMustacheFactory();
    mf.setObjectHandler(mapObjectHandler);

    Mustache m = mf.compile(new StringReader(template), "template");
    StringWriter stringWriter = new StringWriter();
    m.execute(stringWriter, scope);

    assertEquals("", stringWriter.toString());
  }

}
