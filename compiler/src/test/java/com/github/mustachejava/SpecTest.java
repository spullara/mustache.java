package com.github.mustachejava;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import static junit.framework.Assert.assertFalse;

/**
 * Specification tests
 */
public class SpecTest {

  @Test
  public void interpolations() throws IOException {
    run(getSpec("interpolation.yml"));
  }

  @Test
  public void sections() throws IOException {
    run(getSpec("sections.yml"));
  }

  @Test
  public void delimiters() throws IOException {
    run(getSpec("delimiters.yml"));
  }

  @Test
  public void inverted() throws IOException {
    run(getSpec("inverted.yml"));
  }

  @Test
  public void partials() throws IOException {
    run(getSpec("partials.yml"));
  }

  @Test
  public void lambdas() throws IOException {
    run(getSpec("~lambdas.yml"));
  }

  // @Test — need this to appear in the spec repository to enable
  public void inheritance() throws IOException {
    run(getSpec("inheritance.yml"));
  }

  private void run(JsonNode spec) {
    int fail = 0;
    int success = 0;
    int whitespace = 0;
    Map<String, Object> functionMap = new HashMap<String, Object>() {{
      put("Interpolation", new Object() {
        Function lambda() {
          return input -> "world";
        }
      });
      put("Interpolation - Expansion", new Object() {
        Function lambda() {
          return input -> "{{planet}}";
        }
      });
      put("Interpolation - Alternate Delimiters", new Object() {
        Function lambda() {
          return input -> "|planet| => {{planet}}";
        }
      });
      put("Interpolation - Multiple Calls", new Object() {
        int calls = 0;

        Function lambda() {
          return input -> String.valueOf(++calls);
        }
      });
      put("Escaping", new Object() {
        Function lambda() {
          return input -> ">";
        }
      });
      put("Section", new Object() {
        Function lambda() {
          return new TemplateFunction() {
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
          return input -> false;
        }
      });
    }};
    for (final JsonNode test : spec.get("tests")) {
      boolean failed = false;
      final DefaultMustacheFactory CF = createMustacheFactory(test);
      String file = test.get("name").asText();
      System.out.print("Running " + file + " - " + test.get("desc").asText());
      StringReader template = new StringReader(test.get("template").asText());
      JsonNode data = test.get("data");
      try {
        Mustache compile = CF.compile(template, file);
        StringWriter writer = new StringWriter();
        compile.execute(writer, new Object[]{new ObjectMapper().readValue(data.toString(), Map.class), functionMap.get(file)});
        String expected = test.get("expected").asText();
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

  protected DefaultMustacheFactory createMustacheFactory(final JsonNode test) {
    return new DefaultMustacheFactory("/spec/specs") {
      @Override
      public Reader getReader(String resourceName) {
        JsonNode partial = test.get("partials").get(resourceName);
        return new StringReader(partial == null ? "" : partial.asText());
      }
    };
  }

  private JsonNode getSpec(String spec) throws IOException {
    return new YAMLFactory(new YAMLMapper()).createParser(new InputStreamReader(
            SpecTest.class.getResourceAsStream(
                    "/spec/specs/" + spec))).readValueAsTree();
  }

}
