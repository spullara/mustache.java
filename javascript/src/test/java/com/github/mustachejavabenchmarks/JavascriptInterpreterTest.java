package com.github.mustachejavabenchmarks;

import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.JavascriptObjectHandler;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class JavascriptInterpreterTest extends JsonInterpreterTest {
  @Override
  protected Object getScope() throws IOException {
    ScriptEngineManager sem = new ScriptEngineManager();
    ScriptEngine se = sem.getEngineByName("nashorn");
    InputStream json = getClass().getClassLoader().getResourceAsStream("hogan.json");
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    byte[] bytes = new byte[1024];
    int read;
    while ((read = json.read(bytes)) != -1) {
      baos.write(bytes, 0, read);
    }
    try {
      return se.eval("var tweet = " + new String(baos.toByteArray()) + "; " +
              "var tweets = []; for (var i = 0; i < 50; i++) { tweets.push(tweet); };" +
              "this;");
    } catch (ScriptException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  protected DefaultMustacheFactory createMustacheFactory() {
    DefaultMustacheFactory mustacheFactory = new DefaultMustacheFactory(root);
    mustacheFactory.setObjectHandler(new JavascriptObjectHandler());
    return mustacheFactory;
  }
}
