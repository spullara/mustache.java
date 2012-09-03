package com.github.mustachejavabenchmarks;

import com.github.mustachejava.*;
import junit.framework.TestCase;

import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.util.concurrent.Executors;

/**
 * Compare compilation with interpreter.
 * <p/>
 * User: sam
 * Date: 5/14/11
 * Time: 9:28 PM
 */
public class BenchmarkTest extends TestCase {
  private static final int TIME = 2000;
  protected File root;

  protected void setUp() throws Exception {
    super.setUp();
    File file = new File("src/test/resources");
    root = new File(file, "simple.html").exists() ? file : new File("../src/test/resources");

  }

  public static boolean skip() {
    return System.getenv().containsKey("CI") || System.getProperty("CI") != null;
  }

  public void testCompiler() {
    if (skip()) return;
    System.out.println("complex.html compilations per second:");
    for (int i = 0; i < 3; i++) {
      {
        long start = System.currentTimeMillis();
        int total = 0;
        while (true) {
          DefaultMustacheFactory cf = createMustacheFactory();
          Mustache m = cf.compile("complex.html");
          total++;
          if (System.currentTimeMillis() - start > TIME) break;
        }
        System.out.println("Result: " + total * 1000 / TIME);
      }
    }
  }

  public void testComplex() throws MustacheException, IOException {
    if (skip()) return;
    System.out.println("complex.html evaluations per millisecond:");
    for (int i = 0; i < 3; i++) {
      {
        DefaultMustacheFactory cf = createMustacheFactory();
        Mustache m = cf.compile("complex.html");
        ComplexObject complexObject = new ComplexObject();
        complextest(m, complexObject).toString();
        long start = System.currentTimeMillis();
        int total = 0;
        while (true) {
          complextest(m, complexObject);
          total++;
          if (System.currentTimeMillis() - start > TIME) break;
        }
        System.out.println("Serial: " + total / TIME);
      }
    }
  }

  protected DefaultMustacheFactory createMustacheFactory() {
    return new DefaultMustacheFactory();
  }

  public void testComplexFlapping() throws MustacheException, IOException {
    if (skip()) return;
    System.out.println("complex.html evaluations with 3 different objects per millisecond:");
    for (int i = 0; i < 3; i++) {
      {
        DefaultMustacheFactory cf = createMustacheFactory();
        Mustache m = cf.compile("complex.html");
        ComplexObject complexObject = new ComplexObject();
        ComplexObject complexObject2 = new ComplexObject() {};
        ComplexObject complexObject3 = new ComplexObject() {};
        complextest(m, complexObject).toString();
        long start = System.currentTimeMillis();
        int total = 0;
        while (true) {
          complextest(m, total % 3 == 0 ? complexObject : total % 3 == 1 ? complexObject2 : complexObject3);
          total++;
          if (System.currentTimeMillis() - start > TIME) break;
        }
        System.out.println("Serial: " + total / TIME);
      }
    }
  }

  public void testParallelComplex() throws MustacheException, IOException {
    if (skip()) return;
    System.out.println("complex.html evaluations per millisecond:");
    for (int i = 0; i < 3; i++) {
      {
        DefaultMustacheFactory cf = createMustacheFactory();
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
    if (skip()) return;
    System.out.println("complex.html evaluations per millisecond:");
    for (int i = 0; i < 3; i++) {
      {
        MustacheFactory cf = createMustacheFactory();
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
