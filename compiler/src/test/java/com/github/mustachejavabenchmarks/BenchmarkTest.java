package com.github.mustachejavabenchmarks;

import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.util.concurrent.Executors;

import com.github.mustachejava.ComplexObject;
import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.Mustache;
import com.github.mustachejava.MustacheException;
import com.github.mustachejava.MustacheFactory;
import com.github.mustachejava.ParallelComplexObject;
import junit.framework.TestCase;

/**
 * Compare compilation with interpreter.
 * <p/>
 * User: sam
 * Date: 5/14/11
 * Time: 9:28 PM
 */
public class BenchmarkTest extends TestCase {
  private static final int TIME = 2000;
  private File root;

  protected void setUp() throws Exception {
    super.setUp();
    File file = new File("src/test/resources");
    root = new File(file, "simple.html").exists() ? file : new File("../src/test/resources");
  }

  public void testComplex() throws MustacheException, IOException {
    System.out.println("complex.html evaluations per millisecond:");
    for (int i = 0; i < 3; i++) {
      {
        DefaultMustacheFactory cf = new DefaultMustacheFactory();
        Mustache m = cf.compile("complex.html");
        complextest(m, new ComplexObject()).toString();
        long start = System.currentTimeMillis();
        int total = 0;
        while (true) {
          complextest(m, new ComplexObject());
          total++;
          if (System.currentTimeMillis() - start > TIME) break;
        }
        System.out.println("Serial: " + total / TIME);
      }
    }
  }

  public void
  testParallelComplex() throws MustacheException, IOException {
    System.out.println("complex.html evaluations per millisecond:");
    for (int i = 0; i < 3; i++) {
      {
        DefaultMustacheFactory cf = new DefaultMustacheFactory();
        cf.setExecutorService(Executors.newCachedThreadPool());
        Mustache m = cf.compile("complex.html");
        complextest(m, new ParallelComplexObject()).toString();
        long start = System.currentTimeMillis();
        int total = 0;
        while (true) {
          complextest(m, new ParallelComplexObject());
          total++;
          if (System.currentTimeMillis() - start > TIME) break;
        }
        System.out.println("Parallel: " + total / TIME);
      }
    }
  }

  public void testParallelComplexNoExecutor() throws MustacheException, IOException {
    System.out.println("complex.html evaluations per millisecond:");
    for (int i = 0; i < 3; i++) {
      {
        MustacheFactory cf = new DefaultMustacheFactory();
        Mustache m = cf.compile("complex.html");
        complextest(m, new ParallelComplexObject()).toString();
        long start = System.currentTimeMillis();
        int total = 0;
        while (true) {
          complextest(m, new ParallelComplexObject());
          total++;
          if (System.currentTimeMillis() - start > TIME) break;
        }
        System.out.println("Serial with callable: " + total / TIME);
      }
    }
  }

  private Writer complextest(Mustache m, Object complexObject) throws MustacheException, IOException {
    Writer sw = new NullWriter();
    m.execute(sw, complexObject).close();
    return sw;
  }

  public static void main(String[] args) throws Exception {
    BenchmarkTest benchmarkTest = new BenchmarkTest();
    benchmarkTest.setUp();
    benchmarkTest.testComplex();
    benchmarkTest.testParallelComplex();
    benchmarkTest.testParallelComplexNoExecutor();
    System.exit(0);
  }

}
