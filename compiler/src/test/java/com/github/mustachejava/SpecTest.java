package com.github.mustachejava;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.github.mustachejava.impl.DefaultCode;
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

  private JsonFactory jf = new MappingJsonFactory();

  @Test
  public void interpolations() throws IOException {
    run(getSpec("interpolation.json"));
  }

  @Test
  public void sections() throws IOException {
    run(getSpec("sections.json"));
  }

  @Test
  public void delimiters() throws IOException {
    run(getSpec("delimiters.json"));
  }

  @Test
  public void inverted() throws IOException {
    run(getSpec("inverted.json"));
  }

  @Test
  public void partials() throws IOException {
    run(getSpec("partials.json"));
  }

  @Test
  public void lambdas() throws IOException {
    run(getSpec("~lambdas.json"));
  }

  private void run(JsonNode spec) {
    int fail = 0;
    int success = 0;
    for (final JsonNode test : spec.get("tests")) {
      boolean failed = false;      
      DefaultCodeFactory CF = new DefaultCodeFactory("/spec/specs") {
        Map<String, Mustache> partialMap = new HashMap<String, Mustache>();
        JsonNode partials = test.get("partials");
        MustacheCompiler MC = new MustacheCompiler(this);
        {
          MC.setSpecCompliance(true);
        }

        @Override
        public Code partial(final String variable, String file, int line, String sm, String em) {
          return new DefaultCode(null, variable, ">", sm, em) {
            @Override
            public void execute(Writer writer, List<Object> scopes) {
              JsonNode partialNode = partials.get(variable);
              if (partialNode != null && !partialNode.isNull()) {
                String partialName = partialNode.asText();
                Mustache partial = partialMap.get(partialName);
                if (partial == null) {
                  partial = MC.compile(new StringReader(partialName), variable);
                  partialMap.put(partialName, partial);
                }
                partial.execute(writer, scopes);
              }
              appendText(writer);
            }
          };
        }
      };
      MustacheCompiler MC = new MustacheCompiler(CF);
      MC.setSpecCompliance(true);
      String file = test.get("name").getTextValue();
      System.out.print("Running " + file + " - " + test.get("desc").getTextValue());
      StringReader template = new StringReader(test.get("template").getTextValue());
      JsonNode data = test.get("data");
      try {
        Mustache compile = MC.compile(template, file);
        StringWriter writer = new StringWriter();
        compile.execute(writer, Arrays.asList((Object) new JsonMap(data)));
        String expected = test.get("expected").getTextValue();
        if (writer.toString().replaceAll("\\s+", "").equals(expected.replaceAll("\\s+", ""))) {
          System.out.print(": success");
          if (writer.toString().equals(expected)) {
            System.out.println("!");
          } else {
            System.out.println(", whitespace differences.");
          }
        } else {
          System.out.println(": failed!");
          System.out.println(expected + " != " + writer.toString());
          System.out.println(test);
          failed = true;
        }
      } catch (Throwable e) {
        System.out.println(": exception");
        e.printStackTrace();
        System.out.println(test);
        failed = true;
      }
      if (failed) fail++;
      else success++;
    }
    System.out.println("Success: " + success + " Fail: " + fail);
    assertFalse(fail > 0);
  }

  private JsonNode getSpec(String spec) throws IOException {
    return jf.createJsonParser(new InputStreamReader(
            SpecTest.class.getResourceAsStream(
                    "/spec/specs/" + spec))).readValueAsTree();
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
