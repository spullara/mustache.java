package com.github.mustachejava;

import org.junit.Test;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;

import static org.junit.Assert.assertEquals;

public class DelimiterTest {
  @Test
  public void testMavenDelimiter() throws IOException {
    DefaultMustacheFactory mf = new DefaultMustacheFactory();
    Mustache maven = mf.compile(new StringReader("Hello, ${foo}."), "maven", "${", "}");
    StringWriter sw = new StringWriter();
    maven.execute(sw, new Object() {
      String foo = "Jason";
    }).close();
    assertEquals("Hello, Jason.", sw.toString());
  }

  @Test
  public void testAntDelimiter() throws IOException {
    DefaultMustacheFactory mf = new DefaultMustacheFactory();
    Mustache maven = mf.compile(new StringReader("Hello, @foo@."), "maven", "@", "@");
    StringWriter sw = new StringWriter();
    maven.execute(sw, new Object() {
      String foo = "Jason";
    }).close();
    assertEquals("Hello, Jason.", sw.toString());
  }
}
