package com.github.mustachejava;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;

/**
 * Make sure this works.
 * <p/>
 * User: sam
 * Date: 2/19/13
 * Time: 6:38 PM
 */
public class RoudtripMustacheVisitorTest extends BaseTestCase {
  @Override
  protected DefaultMustacheFactory createMustacheFactory() {
    DefaultMustacheFactory mustacheFactory = new DefaultMustacheFactory(root) {
      @Override
      public MustacheVisitor createMustacheVisitor() {
        return new RoundtripMustacheVisitor(this);
      }
    };
    return mustacheFactory;
  }

  public void testPartial() throws IOException {
    MustacheFactory c = init();
    Mustache m = c.compile(new StringReader("{{>simple}}"), "simple.html");
    StringWriter sw = new StringWriter();
    m.execute(sw, new Object() {
      String name = "Chris";
      int value = 10000;

      int taxed_value() {
        return (int) (this.value - (this.value * 0.4));
      }

      boolean in_ca = true;
    });
    assertEquals(getContents(root, "roundtripsimple.txt"), sw.toString());
  }
}
