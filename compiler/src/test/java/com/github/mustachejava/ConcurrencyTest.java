package com.github.mustachejava;

import org.junit.Test;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.security.SecureRandom;
import java.util.Random;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicInteger;

import static com.github.mustachejavabenchmarks.BenchmarkTest.skip;
import static junit.framework.Assert.assertEquals;

/**
 * Inspired by an unconfirmed bug report.
 */
public class ConcurrencyTest {

  static Random r = new SecureRandom();

  private static class TestObject {
    final int a;
    final int b;
    final int c;

    int aa() throws InterruptedException {
      Thread.sleep(r.nextInt(10));
      return a;
    }

    int bb() throws InterruptedException {
      Thread.sleep(r.nextInt(10));
      return b;
    }

    int cc() throws InterruptedException {
      Thread.sleep(r.nextInt(10));
      return c;
    }

    Callable<Integer> calla() throws InterruptedException {
      return () -> {
        Thread.sleep(r.nextInt(10));
        return a;
      };
    }

    Callable<Integer> callb() throws InterruptedException {
      return () -> {
        Thread.sleep(r.nextInt(10));
        return b;
      };
    }

    Callable<Integer> callc() throws InterruptedException {
      return () -> {
        Thread.sleep(r.nextInt(10));
        return c;
      };
    }

    private TestObject(int a, int b, int c) {
      this.a = a;
      this.b = b;
      this.c = c;
    }
  }

  // Alternate render
  static String render(TestObject to) {
    return to.a + ":" + to.b + ":" + to.c;
  }

  @Test
  public void testConcurrentExecution() throws InterruptedException {
    if (skip()) return;
    String template = "{{aa}}:{{bb}}:{{cc}}";
    final Mustache test = new DefaultMustacheFactory().compile(new StringReader(template), "test");
    ExecutorService es = Executors.newCachedThreadPool();
    final AtomicInteger total = render(test, es);
    assertEquals(0, total.intValue());
  }

  private AtomicInteger render(Mustache test, ExecutorService es) throws InterruptedException {
    final AtomicInteger total = new AtomicInteger();
    final Semaphore semaphore = new Semaphore(100);
    for (int i = 0; i < 100000; i++) {
      semaphore.acquire();
      es.submit(() -> {
        try {
        TestObject testObject = new TestObject(r.nextInt(), r.nextInt(), r.nextInt());
        StringWriter sw = new StringWriter();
          test.execute(sw, testObject).close();
          if (!render(testObject).equals(sw.toString())) {
            total.incrementAndGet();
          }
        } catch (IOException e) {
          // Can't fail
          e.printStackTrace();
          System.exit(1);
        } finally {
          semaphore.release();
        }
      });
    }
    // Wait for them all to complete
    semaphore.acquire(100);
    return total;
  }

  @Test
  public void testConcurrentExecutionWithConcurrentTemplate() throws InterruptedException {
    if (skip()) return;
    String template = "{{calla}}:{{callb}}:{{callc}}";
    ExecutorService es = Executors.newCachedThreadPool();
    DefaultMustacheFactory dmf = new DefaultMustacheFactory();
    dmf.setExecutorService(es);
    final Mustache test = dmf.compile(new StringReader(template), "test");
    final AtomicInteger total = render(test, es);
    assertEquals(0, total.intValue());
  }
}
