package com.github.mustachejava;

import org.junit.Test;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;

import static org.junit.Assert.assertEquals;

public class DelimiterTest {
  @Test
  public void testMavenDelimiter() throws IOException {
    DefaultMustacheFactory mf = new NoEncodingMustacheFactory();
    Mustache maven = mf.compile(new StringReader("Hello, ${foo}."), "maven", "${", "}");
    StringWriter sw = new StringWriter();
    maven.execute(sw, new Object() {
      String foo = "Jason";
    }).close();
    assertEquals("Hello, Jason.", sw.toString());
  }

  @Test
  public void testAntDelimiter() throws IOException {
    DefaultMustacheFactory mf = new NoEncodingMustacheFactory();
    Mustache maven = mf.compile(new StringReader("Hello, @foo@."), "maven", "@", "@");
    StringWriter sw = new StringWriter();
    maven.execute(sw, new Object() {
      String foo = "Jason";
    }).close();
    assertEquals("Hello, Jason.", sw.toString());
  }

  @Test
  public void testWithTemplateFunction() throws IOException {
    DefaultMustacheFactory mf = new DefaultMustacheFactory();
    Mustache maven = mf.compile(new StringReader("Hello, ${#f}${foo}${/f}."), "maven", "${", "}");
    StringWriter sw = new StringWriter();
    maven.execute(sw, new Object() {
      TemplateFunction f = (s) -> s;
      String foo = "Jason";
    }).close();
    assertEquals("Hello, Jason.", sw.toString());
  }

  @Test
  public void testWithTemplateFunction2() throws IOException {
    DefaultMustacheFactory mf = new DefaultMustacheFactory();
    Mustache maven = mf.compile(new StringReader("Hello, ${foo}."), "maven", "${", "}");
    StringWriter sw = new StringWriter();
    maven.execute(sw, new Object() {
      TemplateFunction foo = (s) -> "${name}";
      String name = "Jason";
    }).close();
    assertEquals("Hello, ${name}.", sw.toString());
  }

  @Test
  public void testStrSubstitutor() throws IOException {
    DefaultMustacheFactory mf = new DefaultMustacheFactory();
    Mustache maven = mf.compile(new StringReader("Hello, $<foo>."), "maven", "$<", ">");
    StringWriter sw = new StringWriter();
    maven.execute(sw, new Object() {
      String foo = "Jason";
    }).close();
    assertEquals("Hello, Jason.", sw.toString());
  }

  @Test
  public void testStrSubstitutor2() throws IOException {
    DefaultMustacheFactory mf = new DefaultMustacheFactory();
    Mustache maven = mf.compile(new StringReader("{{=$< >=}}Hello, $<foo>."), "maven");
    StringWriter sw = new StringWriter();
    maven.execute(sw, new Object() {
      String foo = "Jason";
    }).close();
    assertEquals("Hello, Jason.", sw.toString());
  }

  @Test
  public void testTemporarilyChangeDelimiters() throws IOException {
    DefaultMustacheFactory mf = new DefaultMustacheFactory();
    Mustache maven = mf.compile(new StringReader("AND {{tfield}} = {{=$< >=}}#{$<={{ }}=>{{name}}.{{{jfield}}}}"), "maven");
    StringWriter sw = new StringWriter();
    maven.execute(sw, new Object() {
      String tfield = "someOtherId";
      String name = "id";
      String jfield = "1";
    }).close();
    assertEquals("AND someOtherId = #{id.1}", sw.toString());
  }

  private static class NoEncodingMustacheFactory extends DefaultMustacheFactory {
    @Override
    public void encode(String value, Writer writer) {
      // TODO: encoding rules
      try {
        writer.write(value);
      } catch (IOException e) {
        throw new MustacheException(e);
      }
    }
  }
}
