package com.github.mustachejava;

import com.github.mustachejava.indy.IndyObjectHandler;
import com.google.common.base.Function;
import org.codehaus.jackson.JsonFactory;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.MappingJsonFactory;
import org.junit.Test;

import java.io.*;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

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
    int whitespace = 0;
    Map<String, Object> functionMap = new HashMap<String, Object>() {{
      put("Interpolation", new Object() {
        Function lambda() {
          return new Function<String, String>() {
            @Override
            public String apply(String input) {
              return "world";
            }
          };
        }
      });
      put("Interpolation - Expansion", new Object() {
        Function lambda() {
          return new Function<String, String>() {
            @Override
            public String apply(String input) {
              return "{{planet}}";
            }
          };
        }
      });
      put("Interpolation - Alternate Delimiters", new Object() {
        Function lambda() {
          return new Function<String, String>() {
            @Override
            public String apply(String input) {
              return "|planet| => {{planet}}";
            }
          };
        }
      });
      put("Interpolation - Multiple Calls", new Object() {
        int calls = 0;
        Function lambda() {
          return new Function<String, String>() {
            @Override
            public String apply(String input) {
              return String.valueOf(++calls);
            }
          };
        }
      });
      put("Escaping", new Object() {
        Function lambda() {
          return new Function<String, String>() {
            @Override
            public String apply(String input) {
              return ">";
            }
          };
        }
      });
      put("Section", new Object() {
        Function lambda() {
          return new Function<String, String>() {
            @Override
            public String apply(String input) {
              return input.equals("{{x}}") ? "yes" : "no";
            }
          };
        }
      });
      put("Section - Expansion", new Object() {
        Function lambda() {
          return new TemplateFunction() {
            @Override
            public String apply(String input) {
              return input + "{{planet}}" + input;
            }
          };
        }
      });
      put("Section - Alternate Delimiters", new Object() {
        Function lambda() {
          return new TemplateFunction() {
            @Override
            public String apply(String input) {
              return input + "{{planet}} => |planet|" + input;
            }
          };
        }
      });
      put("Section - Multiple Calls", new Object() {
        Function lambda() {
          return new Function<String, String>() {
            @Override
            public String apply(String input) {
              return "__" + input + "__";
            }
          };
        }
      });
      put("Inverted Section", new Object() {
        Function lambda() {
          return new Function<String, Object>() {
            @Override
            public Object apply(String input) {
              return false;
            }
          };
        }
      });
    }}; 
    for (final JsonNode test : spec.get("tests")) {
      boolean failed = false;      
      final DefaultMustacheFactory CF = new DefaultMustacheFactory("/spec/specs") {
        @Override
        public Reader getReader(String resourceName) {
          JsonNode partial = test.get("partials").get(resourceName);
          return new StringReader(partial == null ? "" : partial.getTextValue());
        }
      };
      CF.setObjectHandler(new IndyObjectHandler());
      MustacheParser MC = new MustacheParser(CF);
      String file = test.get("name").getTextValue();
      System.out.print("Running " + file + " - " + test.get("desc").getTextValue());
      StringReader template = new StringReader(test.get("template").getTextValue());
      JsonNode data = test.get("data");
      try {
        Mustache compile = MC.compile(template, file);
        StringWriter writer = new StringWriter();        
        compile.execute(writer, new Object[] { new JsonMap(data), functionMap.get(file) });
        String expected = test.get("expected").getTextValue();
        if (writer.toString().replaceAll("\\s+", "").equals(expected.replaceAll("\\s+", ""))) {
          System.out.print(": success");
          if (writer.toString().equals(expected)) {
            System.out.println("!");
          } else {
            whitespace++;
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
    System.out.println("Success: " + success + " Whitespace: " + whitespace + " Fail: " + fail);
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
