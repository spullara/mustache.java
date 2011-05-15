package com.sampullara.mustache;

import com.sampullara.util.FutureWriter;
import junit.framework.TestCase;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.util.concurrent.ExecutionException;

/**
 * Tests for the compiler.
 * <p/>
 * User: sam
 * Date: May 3, 2010
 * Time: 10:23:54 AM
 */
public class InterpreterTest extends TestCase {
  private File root;

  public void testSimple() throws MustacheException, IOException, ExecutionException, InterruptedException {
    MustacheInterpreter c = new MustacheInterpreter(root);
    Mustache mustache = c.compile(new BufferedReader(new FileReader(new File(root, "simple.html"))));
    StringWriter sw = runtest(mustache);
    assertEquals(getContents(root, "simple.txt"), sw.toString());
  }

  private StringWriter runtest(Mustache mustache) throws MustacheException, IOException {
    StringWriter sw = new StringWriter();
    FutureWriter writer = new FutureWriter(sw);
    mustache.execute(writer, new Scope(new Object() {
      String name = "Chris";
      int value = 10000;

      int taxed_value() {
        return (int) (this.value - (this.value * 0.4));
      }

      boolean in_ca = true;
    }));
    writer.flush();
    return sw;
  }

  public void testBenchmark() throws MustacheException, IOException {
    {
      long compile = System.currentTimeMillis();
      MustacheInterpreter c = new MustacheInterpreter(root);
      Mustache mustache = c.compile(new BufferedReader(new FileReader(new File(root, "simple.html"))));
      System.out.println(System.currentTimeMillis() - compile);
      StringWriter sw = runtest(mustache);
      long start = System.currentTimeMillis();
      for (int i = 0; i < 100000; i++) {
        runtest(mustache);
      }
      System.out.println(System.currentTimeMillis() - start);
    }
    {
      long compile = System.currentTimeMillis();
      MustacheCompiler c = new MustacheCompiler(root);
      Mustache mustache = c.compile(new BufferedReader(new FileReader(new File(root, "simple.html"))));
      System.out.println(System.currentTimeMillis() - compile);
      StringWriter sw = runtest(mustache);
      long start = System.currentTimeMillis();
      for (int i = 0; i < 100000; i++) {
        runtest(mustache);
      }
      System.out.println(System.currentTimeMillis() - start);
    }
    {
      long compile = System.currentTimeMillis();
      MustacheInterpreter c = new MustacheInterpreter(root);
      Mustache mustache = c.compile(new BufferedReader(new FileReader(new File(root, "simple.html"))));
      System.out.println(System.currentTimeMillis() - compile);
      StringWriter sw = runtest(mustache);
      long start = System.currentTimeMillis();
      for (int i = 0; i < 100000; i++) {
        runtest(mustache);
      }
      System.out.println(System.currentTimeMillis() - start);
    }
    {
      long compile = System.currentTimeMillis();
      MustacheCompiler c = new MustacheCompiler(root);
      Mustache mustache = c.compile(new BufferedReader(new FileReader(new File(root, "simple.html"))));
      System.out.println(System.currentTimeMillis() - compile);
      StringWriter sw = runtest(mustache);
      long start = System.currentTimeMillis();
      for (int i = 0; i < 100000; i++) {
        runtest(mustache);
      }
      System.out.println(System.currentTimeMillis() - start);
    }
  }

  protected String getContents(File root, String file) throws IOException {
    BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(new File(root, file)),"UTF-8"));
    StringWriter capture = new StringWriter();
    char[] buffer = new char[8192];
    int read;
    while ((read = br.read(buffer)) != -1) {
      capture.write(buffer, 0, read);
    }
    return capture.toString();
  }

  protected void setUp() throws Exception {
    super.setUp();
    root = new File("src/test/resources");
  }
}
