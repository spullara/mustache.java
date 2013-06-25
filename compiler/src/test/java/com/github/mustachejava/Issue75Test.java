package com.github.mustachejava;

import org.junit.Test;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;

/**
 * Reproduction test case
 */
public class Issue75Test {
  @Test
  public void testDotNotationWithNull() throws IOException {
    DefaultMustacheFactory mf = new DefaultMustacheFactory();
    Mustache m = mf.compile(new StringReader("[{{category.name}}]"), "test");
    StringWriter sw = new StringWriter();
    Map map = new HashMap();
    map.put("category", null);
    m.execute(sw, map).close();
  }
}
