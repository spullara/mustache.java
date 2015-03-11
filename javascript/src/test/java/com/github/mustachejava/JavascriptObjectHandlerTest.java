package com.github.mustachejava;

import com.github.mustachejavabenchmarks.Tweet;
import org.junit.BeforeClass;
import org.junit.Test;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import java.io.*;
import java.util.ArrayList;
import java.util.List;

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
  
  @Test
  public void testTweet() throws IOException, ScriptException {
    File file = new File("src/test/resources");
    File root = new File(file, "simple.html").exists() ? file : new File("../src/test/resources");
    DefaultMustacheFactory mf = new DefaultMustacheFactory(root);
    mf.setObjectHandler(new JavascriptObjectHandler());
    InputStream json = getClass().getClassLoader().getResourceAsStream("hogan.json");
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    byte[] bytes = new byte[1024];
    int read;
    while ((read = json.read(bytes)) != -1) {
      baos.write(bytes, 0, read);
    }
    Object scope = se.eval("var tweet = " + new String(baos.toByteArray()) + "; " +
            "var tweets = []; for (var i = 0; i < 50; i++) { tweets.push(tweet); };" +
            "this;");
    StringWriter sw = new StringWriter();
    Mustache m = mf.compile("timeline.mustache");
    m.execute(sw, scope).close();

    StringWriter sw2 = new StringWriter();
    final List<Tweet> list = new ArrayList<Tweet>();
    for (int i = 0; i < 50 ; i++) {
      list.add(new Tweet());
    }
    m.execute(sw2, new Object() {
      List tweets = list;
    }).close();
    assertEquals(sw2.toString(), sw.toString());
  }
}
