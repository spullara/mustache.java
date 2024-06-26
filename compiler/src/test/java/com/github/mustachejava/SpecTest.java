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
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import static org.junit.Assert.assertFalse;

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

  @Test
  public void inheritance() throws IOException {
    run(getSpec("~inheritance.yml"));
  }

  private void run(JsonNode spec) {
    int fail = 0;
    int success = 0;
    int whitespace = 0;
    Map<String, Object> functionMap = new HashMap<String, Object>() {{
      put("Interpolation", singletonMapWithConstantLambda("world"));
      put("Interpolation - Expansion", singletonMapWithConstantLambda("{{planet}}"));
      put("Interpolation - Alternate Delimiters", singletonMapWithConstantLambda("|planet| => {{planet}}"));
      put("Interpolation - Multiple Calls",singletonMap("lambda", new Function<String, String>() {

        int calls = 0;

        @Override
        public String apply(String input) {
          return String.valueOf(++calls);
        }
      }));
      put("Escaping", singletonMapWithConstantLambda(">"));
      put("Section", singletonMap(
          "lambda",
          (TemplateFunction) (input) -> input.equals("{{x}}") ? "yes" : "no"));
      put("Section - Expansion", singletonMap(
          "lambda",
          (TemplateFunction) (input) -> input + "{{planet}}" + input));
      put("Section - Alternate Delimiters", singletonMap(
          "lambda",
          (TemplateFunction) (input) -> input + "{{planet}} => |planet|" + input));
      put("Section - Multiple Calls", singletonMap(
          "lambda",
          (TemplateFunction) (input) -> "__" + input + "__"));
      put("Inverted Section", singletonMapWithConstantLambda(false));
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
        String json = data.toString();
        if (json.startsWith("{")) {
          compile.execute(writer, new Object[]{functionMap.get(file), new ObjectMapper().readValue(json, Map.class)});
        } else if (json.startsWith("[")) {
          compile.execute(writer, new Object[]{functionMap.get(file), new ObjectMapper().readValue(json, List.class)});
        } else {
          String s = new ObjectMapper().readValue(json, String.class);
          compile.execute(writer, new Object[]{functionMap.get(file), s});
        }
        String expected = test.get("expected").asText();
        if (transformOutput(writer.toString()).equals(transformOutput(expected))) {
          System.out.print(": success");
          if (writer.toString().equals(expected)) {
            System.out.println("!");
          } else {
            whitespace++;
            System.out.println(", whitespace differences.");
          }
        } else {
          System.out.println(": failed!");
          System.out.println("'" + expected + "' != '" + writer + "'");
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

  protected String transformOutput(String output) {
    return output.replaceAll("\\s+", "");
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

  private static Map<String, Function<String, Object>> singletonMapWithConstantLambda(Object value) {
    return new HashMap<String, Function<String, Object>>() {{
      put("lambda", (input) -> value);
    }};
  }

  private static Map<String, Object> singletonMap(String key, Object value) {
    return new HashMap<String, Object>() {{
      put(key, value);
    }};
  }
}
