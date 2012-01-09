package com.github.mustachejava;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;

import com.github.mustachejava.impl.DefaultCodeFactory;
import org.codehaus.jackson.JsonFactory;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.MappingJsonFactory;
import org.junit.Test;

import static junit.framework.Assert.assertFalse;

/**
 * Specification tests
 */
public class SpecTest {

  private static final MustacheCompiler MC = new MustacheCompiler(new DefaultCodeFactory());
  private JsonFactory jf = new MappingJsonFactory();

  @Test
  public void interpolations() throws IOException {
    run(getSpec("/spec/specs/interpolation.json"));
  }

  @Test
  public void sections() throws IOException {
    run(getSpec("/spec/specs/sections.json"));
  }

  private void run(JsonNode spec) {
    boolean failed = false;
    for (final JsonNode test : spec.get("tests")) {
      String file = test.get("name").getTextValue();
      System.out.print("Running " + file + " - " + test.get("desc").getTextValue());
      StringReader template = new StringReader(test.get("template").getTextValue());
      JsonNode data = test.get("data");
      Mustache compile = MC.compile(template, file);
      StringWriter writer = new StringWriter();
      try {
        compile.execute(writer, Arrays.asList((Object) new JsonMap(data)));
        String expected = test.get("expected").getTextValue();
        if (writer.toString().equals(expected)) {
          System.out.println(": success!");
        } else {
          System.out.println(": failed!");
          System.out.println(expected + " != " + writer.toString());
          System.out.println(test);
          failed = true;
        }
      } catch (Exception e) {
        System.out.println(": exception: " + e);
        System.out.println(test);
        failed = true;
      }
    }
    assertFalse(failed);
  }

  private JsonNode getSpec(String spec) throws IOException {
    return jf.createJsonParser(new InputStreamReader(
            SpecTest.class.getResourceAsStream(
                    spec))).readValueAsTree();
  }

  private static class JsonMap extends HashMap {
    private final JsonNode test;

    public JsonMap(JsonNode test) {
      this.test = test;
    }

    @Override
    public Object get(Object key) {
      JsonNode value = test.get(key.toString());
      return convert(value);
    }

    private Object convert(final JsonNode value) {
      if (value == null || value.isNull()) return null;
      if (value.isBoolean()) {
        return value.getBooleanValue();
      } else if (value.isValueNode()) {
        return value.asText();
      } else if (value.isArray()) {
        return new Iterable() {
          @Override
          public Iterator iterator() {
            return new Iterator() {
              private Iterator<JsonNode> iterator = value.iterator();

              @Override
              public boolean hasNext() {
                return iterator.hasNext();
              }

              @Override
              public Object next() {
                return convert(iterator.next());
              }

              @Override
              public void remove() {
              }
            };
          }
        };
      } else {
        return new JsonMap(value);
      }
    }
  }
}
