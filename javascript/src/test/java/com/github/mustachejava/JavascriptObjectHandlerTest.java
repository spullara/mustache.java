package com.github.mustachejava;

import org.junit.BeforeClass;
import org.junit.Test;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;

import static org.junit.Assert.assertEquals;

/**
 * Created by sam on 3/11/15.
 */
public class JavascriptObjectHandlerTest {

  private static ScriptEngine se;

  @BeforeClass
  public static void setup() {
    ScriptEngineManager sem = new ScriptEngineManager();
    se = sem.getEngineByName("nashorn");
  }
  
  @Test
  public void testSimple() throws ScriptException, IOException {
    MustacheFactory mf = new DefaultMustacheFactory();
    Mustache m = mf.compile(new StringReader("{{name}}"), "test");
    StringWriter sw = new StringWriter();
    Object eval = se.eval("var name = \"sam\"; this;");
    m.execute(sw, eval).close();
    assertEquals("sam", sw.toString());
  }

  @Test
  public void testFunction() throws ScriptException, IOException {
    DefaultMustacheFactory mf = new DefaultMustacheFactory();
    mf.setObjectHandler(new JavascriptObjectHandler());
    Mustache m = mf.compile(new StringReader("{{#f}}name{{/f}}{{value}}"), "test");
    StringWriter sw = new StringWriter();
    Object eval = se.eval("var name = \"sam\"; " +
            "var value = function() { return 'pullara'; };" +
            "function f(s) { return '{{' + s + '}}'; };" +
            "this;");
    m.execute(sw, eval).close();
    assertEquals("sampullara", sw.toString());
  }
}
