package com.github.mustachejava.javascript;

import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.Mustache;
import org.junit.Test;

import javax.script.Invocable;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;

import static org.junit.Assert.assertEquals;

public class NashornObjectHandlerTest {
  @Test
  public void testCoerce() throws Exception {

  }

  @Test
  public void testFindWrapper() throws Exception {

  }

  @Test
  public void testJavascript() throws ScriptException, IOException, NoSuchMethodException {
    ScriptEngine engine = createEngine();
    Object eval = engine.eval("function test() { return 'test' }");
    if (engine instanceof Invocable) {
      Object test = ((Invocable) engine).invokeFunction("test");
      System.out.println(test);
    }
    DefaultMustacheFactory mf = new DefaultMustacheFactory();
    mf.setObjectHandler(new NashornObjectHandler(engine));
    Mustache template = mf.compile(new StringReader("{{test}}"), "test");
    StringWriter writer = new StringWriter();
    template.execute(writer, eval).close();
    assertEquals("test", writer.toString());
  }

  public ScriptEngine createEngine() {
    ScriptEngineManager sem = new ScriptEngineManager();
    return sem.getEngineByName("nashorn");
  }
}
