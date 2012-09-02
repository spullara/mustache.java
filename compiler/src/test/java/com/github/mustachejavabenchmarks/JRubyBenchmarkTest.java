package com.github.mustachejavabenchmarks;

import com.github.mustachejava.ComplexObject;
import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.Mustache;
import com.github.mustachejava.MustacheException;
import com.github.mustachejava.jruby.JRubyObjectHandler;
import org.jruby.embed.ScriptingContainer;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.*;

import static com.github.mustachejavabenchmarks.BenchmarkTest.skip;
import static junit.framework.Assert.assertEquals;

public class JRubyBenchmarkTest {
  private static final int TIME = 2000;
  private static File root;

  @Test
  public void testComplex() throws IOException {
    Object context = makeComplex();
    DefaultMustacheFactory mf = new DefaultMustacheFactory(root);
    mf.setObjectHandler(new JRubyObjectHandler());
    Mustache m = mf.compile("complex.html");
    Writer writer = complextest(m, context);
    assertEquals(getContents(root, "complex.txt"), writer.toString());
  }

  private Object makeComplex() {
    ScriptingContainer sc = new ScriptingContainer();
    return sc.runScriptlet(JRubyBenchmarkTest.class.getResourceAsStream("complex.rb"), "complex.rb");
  }

  @Test
  public void testComplexBench() throws MustacheException, IOException {
    if (skip()) return;
    System.out.println("complex.html evaluations per millisecond:");
    for (int i = 0; i < 3; i++) {
      {
        DefaultMustacheFactory cf = new DefaultMustacheFactory();
        cf.setObjectHandler(new JRubyObjectHandler());
        Mustache m = cf.compile("complex.html");
        Object context = makeComplex();
        assertEquals(getContents(root, "complex.txt"), complextest(m, context).toString());
        long start = System.currentTimeMillis();
        int total = 0;
        while (true) {
          complextest(m, context);
          total++;
          if (System.currentTimeMillis() - start > TIME) break;
        }
        System.out.println("Ruby: " + total / TIME);
      }
      {
        DefaultMustacheFactory cf = new DefaultMustacheFactory();
        Mustache m = cf.compile("complex.html");
        Object context = new ComplexObject();
        assertEquals(getContents(root, "complex.txt"), complextest(m, context).toString());
        long start = System.currentTimeMillis();
        int total = 0;
        while (true) {
          complextest(m, context);
          total++;
          if (System.currentTimeMillis() - start > TIME) break;
        }
        System.out.println("Java: " + total / TIME);
      }
    }
  }

  private Writer complextest(Mustache m, Object context) throws IOException {
    Writer writer = new StringWriter();
    writer = m.execute(writer, context);
    writer.close();
    return writer;
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
