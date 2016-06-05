package com.github.mustachejava.simple;

import com.fasterxml.jackson.databind.JsonNode;
import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.SpecTest;
import com.github.mustachejava.reflect.SimpleObjectHandler;

public class SimpleSpecTest extends SpecTest {
  @Override
  protected DefaultMustacheFactory createMustacheFactory(JsonNode test) {
    DefaultMustacheFactory mf = super.createMustacheFactory(test);
    mf.setObjectHandler(new SimpleObjectHandler());
    return mf;
  }
}
