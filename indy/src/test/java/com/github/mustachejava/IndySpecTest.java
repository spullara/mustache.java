package com.github.mustachejava;

import com.github.mustachejava.indy.IndyObjectHandler;
import org.codehaus.jackson.JsonNode;

/**
 * Specification tests
 */
public class IndySpecTest extends SpecTest {
  @Override
  protected DefaultMustacheFactory createMustacheFactory(JsonNode test) {
    DefaultMustacheFactory mustacheFactory = super.createMustacheFactory(test);
    mustacheFactory.setObjectHandler(new IndyObjectHandler());
    return mustacheFactory;
  }
}
