package com.github.mustachejava;

import com.fasterxml.jackson.databind.JsonNode;

import java.io.Reader;
import java.io.StringReader;

public class FullSpecTest extends SpecTest {
  @Override
  protected DefaultMustacheFactory createMustacheFactory(final JsonNode test) {
    return new SpecMustacheFactory("/spec/specs") {
      @Override
      public Reader getReader(String resourceName) {
        JsonNode partial = test.get("partials").get(resourceName);
        return new StringReader(partial == null ? "" : partial.asText());
      }
    };
  }

  @Override
  protected String transformOutput(String output) {
    return output;
  }
}
