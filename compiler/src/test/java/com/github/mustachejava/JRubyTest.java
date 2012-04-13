package com.github.mustachejava;

import com.github.mustachejava.jruby.JRubyObjectHandler;
import org.jruby.embed.ScriptingContainer;
import org.junit.Test;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;

import static junit.framework.Assert.assertEquals;

public class JRubyTest {
  @Test
  public void testHash() throws IOException {
    ScriptingContainer sc = new ScriptingContainer();
    Object context = sc.runScriptlet("{:test=>'fred'}");
    DefaultMustacheFactory mf = new DefaultMustacheFactory();
    mf.setObjectHandler(new JRubyObjectHandler());
    Mustache m = mf.compile(new StringReader("{{test}}"), "test");
    Writer writer = new StringWriter();
    writer = m.execute(writer, context);
    writer.close();
    assertEquals("fred", writer.toString());
  }

  @Test
  public void testObject() throws IOException {
    ScriptingContainer sc = new ScriptingContainer();
    Object context = sc.runScriptlet("class Test\ndef test()\n  \"fred\"\nend\nend\nTest.new\n");
    DefaultMustacheFactory mf = new DefaultMustacheFactory();
    mf.setObjectHandler(new JRubyObjectHandler());
    Mustache m = mf.compile(new StringReader("{{test}}"), "test");
    Writer writer = new StringWriter();
    writer = m.execute(writer, context);
    writer.close();
    assertEquals("fred", writer.toString());
  }

  @Test
  public void testArray() throws IOException {
    ScriptingContainer sc = new ScriptingContainer();
    Object context = sc.runScriptlet("class Test\ndef test()\n  [\"fred\",\"fred\"]\nend\nend\nTest.new\n");
    DefaultMustacheFactory mf = new DefaultMustacheFactory();
    mf.setObjectHandler(new JRubyObjectHandler());
    Mustache m = mf.compile(new StringReader("{{#test}}{{.}}{{/test}}"), "test");
    Writer writer = new StringWriter();
    writer = m.execute(writer, context);
    writer.close();
    assertEquals("fredfred", writer.toString());
  }
}
