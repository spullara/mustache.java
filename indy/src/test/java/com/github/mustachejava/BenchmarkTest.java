package com.github.mustachejava;

import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.util.concurrent.Executors;

import com.github.mustachejava.indy.IndyObjectHandler;
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
        DefaultMustacheFactory cf = init();
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
        DefaultMustacheFactory cf = init();
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
        MustacheFactory cf = init();
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

  private DefaultMustacheFactory init() {
    DefaultMustacheFactory factory = new DefaultMustacheFactory();
    if (indy) {
      factory.setObjectHandler(new IndyObjectHandler());
    }
    return factory;
  }

  private Writer complextest(Mustache m, Object complexObject) throws MustacheException, IOException {
    Writer sw = new NullWriter();
    m.execute(sw, complexObject).close();
    return sw;
  }

  private boolean indy = true;

  public static void main(String[] args) throws Exception {
    BenchmarkTest benchmarkTest = new BenchmarkTest();
    benchmarkTest.setUp();
    System.out.println("Indy");
    benchmarkTest.testComplex();
    benchmarkTest.indy = false;
    System.out.println("Reflection");
    benchmarkTest.testComplex();
    benchmarkTest.indy = true;
    System.out.println("Indy");
    benchmarkTest.testComplex();
    benchmarkTest.indy = false;
    System.out.println("Reflection");
    benchmarkTest.testComplex();
    System.exit(0);
  }
}
