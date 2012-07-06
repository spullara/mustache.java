package com.github.mustachejava;

import com.github.mustachejava.reflect.SimpleObjectHandler;
import org.codehaus.jackson.JsonNode;

public class SimpleSpecTest extends SpecTest {
  @Override
  protected DefaultMustacheFactory createMustacheFactory(JsonNode test) {
    DefaultMustacheFactory mf = new DefaultMustacheFactory();
    mf.setObjectHandler(new SimpleObjectHandler());
    return mf;
  }
}
