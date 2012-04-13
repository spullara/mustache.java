package com.github.mustachejavabenchmarks;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.io.Writer;

import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.Mustache;
import com.github.mustachejava.jruby.JRubyObjectHandler;
import org.jruby.embed.ScriptingContainer;
import org.junit.BeforeClass;
import org.junit.Test;

import static junit.framework.Assert.assertEquals;

public class JRubyBenchmarkTest {
  private static File root;

  @Test
  public void testComplex() throws IOException {
    ScriptingContainer sc = new ScriptingContainer();
    Object context = sc.runScriptlet(JRubyBenchmarkTest.class.getResourceAsStream("complex.rb"), "complex.rb");
    DefaultMustacheFactory mf = new DefaultMustacheFactory(root);
    mf.setObjectHandler(new JRubyObjectHandler());
    Mustache m = mf.compile("complex.html");
    Writer writer = new StringWriter();
    writer = m.execute(writer, context);
    writer.close();
    assertEquals(getContents(root, "complex.txt"), writer.toString());
  }

  protected String getContents(File root, String file) throws IOException {
    BufferedReader br = new BufferedReader(
            new InputStreamReader(new FileInputStream(new File(root, file)), "UTF-8"));
    StringWriter capture = new StringWriter();
    char[] buffer = new char[8192];
    int read;
    while ((read = br.read(buffer)) != -1) {
      capture.write(buffer, 0, read);
    }
    return capture.toString();
  }

  @BeforeClass
  public static void setUp() throws Exception {
    File file = new File("src/test/resources");
    root = new File(file, "complex.html").exists() ? file : new File("../src/test/resources");
  }
}
