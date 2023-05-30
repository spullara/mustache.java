package com.github.mustachejava;

import com.fasterxml.jackson.databind.JsonNode;
import com.github.mustachejava.reflect.MapObjectHandler;
import com.github.mustachejava.reflect.SimpleObjectHandler;
import org.junit.Test;

public class MapObjectHandlerSpecTest extends SpecTest {
  @Override
  protected DefaultMustacheFactory createMustacheFactory(JsonNode test) {
    DefaultMustacheFactory mf = super.createMustacheFactory(test);
    mf.setObjectHandler(new MapObjectHandler());
    return mf;
  }
}
