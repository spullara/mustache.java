package com.github.mustachejava;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import javax.annotation.Nullable;

import com.github.mustachejava.util.CapturingMustacheVisitor;
import com.github.mustachejavabenchmarks.JsonCapturer;
import com.github.mustachejavabenchmarks.JsonInterpreterTest;
import junit.framework.TestCase;
import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.MappingJsonFactory;

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
    MustacheFactory c = createMustacheFactory();
    Mustache m = c.compile("simple.html");
    StringWriter sw = new StringWriter();
    m.execute(sw, new Object() {
      String name = "Chris";
      int value = 10000;

      int taxed_value() {
        return (int) (this.value - (this.value * 0.4));
      }

      boolean in_ca = true;
    });
    assertEquals(getContents(root, "simple.txt"), sw.toString());
  }

  protected DefaultMustacheFactory createMustacheFactory() {
    return new DefaultMustacheFactory(root);
  }

  public void testSimplePragma() throws MustacheException, IOException, ExecutionException, InterruptedException {
    MustacheFactory c = createMustacheFactory();
    Mustache m = c.compile("simplepragma.html");
    StringWriter sw = new StringWriter();
    m.execute(sw, new Object() {
      String name = "Chris";
      int value = 10000;

      int taxed_value() {
        return (int) (this.value - (this.value * 0.4));
      }

      boolean in_ca = true;
    });
    assertEquals(getContents(root, "simple.txt"), sw.toString());
  }

  public void testMultipleWrappers() throws MustacheException, IOException, ExecutionException, InterruptedException {
    MustacheFactory c = createMustacheFactory();
    Mustache m = c.compile("simple.html");
    StringWriter sw = new StringWriter();
    m.execute(sw, new Object() {
      String name = "Chris";
      int value = 10000;

      Object o = new Object() {
        int taxed_value() {
          return (int) (value - (value * 0.4));
        }

        String fred = "test";
      };

      Object in_ca = Arrays.asList(
              o, new Object() {
        int taxed_value = (int) (value - (value * 0.2));
      },
              o
      );
    });
    assertEquals(getContents(root, "simplerewrap.txt"), sw.toString());
  }

  public void testNestedLatchesIterable() throws IOException {
    DefaultMustacheFactory c = createMustacheFactory();
    c.setExecutorService(Executors.newCachedThreadPool());
    Mustache m = c.compile("latchedtestiterable.html");
    StringWriter sw = new StringWriter();
    final StringBuffer sb = new StringBuffer();
    final CountDownLatch cdl1 = new CountDownLatch(1);
    final CountDownLatch cdl2 = new CountDownLatch(1);

    m.execute(sw, new Object() {
      Iterable list = Arrays.asList(
              new Callable<Object>() {
                @Override
                public Object call() throws Exception {
                  cdl1.await();
                  sb.append("How");
                  return "How";
                }
              },
              new Callable<Object>() {
                @Override
                public Object call() throws Exception {
                  cdl2.await();
                  sb.append("are");
                  cdl1.countDown();
                  return "are";
                }
              },
              new Callable<Object>() {
                @Override
                public Object call() throws Exception {
                  sb.append("you?");
                  cdl2.countDown();
                  return "you?";
                }
              }
      );
    }).close();
    assertEquals(getContents(root, "latchedtest.txt"), sw.toString());
    assertEquals("you?areHow", sb.toString());
  }

  public void testNestedLatches() throws IOException {
    DefaultMustacheFactory c = createMustacheFactory();
    c.setExecutorService(Executors.newCachedThreadPool());
    Mustache m = c.compile("latchedtest.html");
    StringWriter sw = new StringWriter();
    Writer execute = m.execute(sw, new Object() {
      Callable<Object> nest = new Callable<Object>() {
        @Override
        public Object call() throws Exception {
          Thread.sleep(300);
          return "How";
        }
      };
      Callable<Object> nested = new Callable<Object>() {
        @Override
        public Object call() throws Exception {
          Thread.sleep(200);
          return "are";
        }
      };
      Callable<Object> nestest = new Callable<Object>() {
        @Override
        public Object call() throws Exception {
          Thread.sleep(100);
          return "you?";
        }
      };
    });
    execute.close();

    assertEquals("<outer>\n<inner>How</inner>\n<inner>are</inner>\n<inner>you?</inner>\n</outer>\n", sw.toString());
  }

  public void testBrokenSimple() throws MustacheException, IOException, ExecutionException, InterruptedException {
    try {
      MustacheFactory c = init();
      Mustache m = c.compile("brokensimple.html");
      StringWriter sw = new StringWriter();
      m.execute(sw, new Object() {
        String name = "Chris";
        int value = 10000;

        int taxed_value() {
          return (int) (this.value - (this.value * 0.4));
        }

        boolean in_ca = true;
      });
      fail("Should have failed: " + sw.toString());
    } catch (Exception e) {
      // success
    }
  }

  public void testIsNotEmpty() throws IOException {
    MustacheFactory c = createMustacheFactory();
    Mustache m = c.compile("isempty.html");
    StringWriter sw = new StringWriter();
    m.execute(sw, new Object() {
      List people = Arrays.asList("Test");
    });
    assertEquals(getContents(root, "isempty.txt"), sw.toString());
  }

  public void testSecurity() throws MustacheException, IOException, ExecutionException, InterruptedException {
    MustacheFactory c = init();
    Mustache m = c.compile("security.html");
    StringWriter sw = new StringWriter();
    m.execute(sw, new Object() {
      String name = "Chris";
      int value = 10000;

      int taxed_value() {
        return (int) (this.value - (this.value * 0.4));
      }

      boolean in_ca = true;

      // Should not be accessible
      private String test = "Test";
    });
    assertEquals(getContents(root, "security.txt"), sw.toString());
  }

  public void testIdentitySimple() throws MustacheException, IOException, ExecutionException, InterruptedException {
    MustacheFactory c = init();
    Mustache m = c.compile("simple.html");
    StringWriter sw = new StringWriter();
    m.identity(sw);
    assertEquals(getContents(root, "simple.html").replaceAll("\\s+", ""), sw.toString().replaceAll(
            "\\s+", ""));
  }

  public void testProperties() throws MustacheException, IOException, ExecutionException, InterruptedException {
    MustacheFactory c = init();
    Mustache m = c.compile("simple.html");
    StringWriter sw = new StringWriter();
    m.execute(sw, new Object() {
      String getName() {
        return "Chris";
      }

      int getValue() {
        return 10000;
      }

      int taxed_value() {
        return (int) (this.getValue() - (this.getValue() * 0.4));
      }

      boolean isIn_ca() {
        return true;
      }
    });
    assertEquals(getContents(root, "simple.txt"), sw.toString());
  }

  public void testSimpleWithMap() throws MustacheException, IOException, ExecutionException, InterruptedException {
    MustacheFactory c = init();
    Mustache m = c.compile("simple.html");
    StringWriter sw = new StringWriter();
    m.execute(sw, new HashMap<String, Object>() {{
      put("name", "Chris");
      put("value", 10000);
      put("taxed_value", 6000);
      put("in_ca", true);
    }});
    assertEquals(getContents(root, "simple.txt"), sw.toString());
  }

  public void testPartial() throws MustacheException, IOException {
    MustacheFactory c = init();
    Mustache m = c.compile("template_partial.html");
    StringWriter sw = new StringWriter();
    Map scope = new HashMap();
    scope.put("title", "Welcome");
    scope.put("template_partial_2", new Object() {
      String again = "Goodbye";
    });
    m.execute(sw, scope);
    assertEquals(getContents(root, "template_partial.txt"), sw.toString());
  }

  public void testPartialWithTF() throws MustacheException, IOException {
    MustacheFactory c = init();
    Mustache m = c.compile("partialintemplatefunction.html");
    StringWriter sw = new StringWriter();
    m.execute(sw, new Object() {
      public TemplateFunction i() {
        return new TemplateFunction() {
          @Override
          public String apply(@Nullable String s) {
            return s;
          }
        };
      }
    });
    assertEquals("This is not interesting.", sw.toString());
  }

  public void testComplex() throws MustacheException, IOException {
    StringWriter json = new StringWriter();
    MappingJsonFactory jf = new MappingJsonFactory();
    final JsonGenerator jg = jf.createJsonGenerator(json);
    jg.writeStartObject();
    final JsonCapturer captured = new JsonCapturer(jg);
    MustacheFactory c = new DefaultMustacheFactory(root) {
      @Override
      public MustacheVisitor createMustacheVisitor() {
        return new CapturingMustacheVisitor(this, captured);
      }
    };
    Mustache m = c.compile("complex.html");
    StringWriter sw = new StringWriter();
    m.execute(sw, new ComplexObject());
    jg.writeEndObject();
    jg.flush();
    assertEquals(getContents(root, "complex.txt"), sw.toString());
    JsonNode jsonNode = jf.createJsonParser(json.toString()).readValueAsTree();
    Object o = JsonInterpreterTest.toObject(jsonNode);
    sw = new StringWriter();
    m = init().compile("complex.html");
    m.execute(sw, o);
    assertEquals(getContents(root, "complex.txt"), sw.toString());
  }

  public void testComplexParallel() throws MustacheException, IOException {
    MustacheFactory c = initParallel();
    Mustache m = c.compile("complex.html");
    StringWriter sw = new StringWriter();
    m.execute(sw, new ParallelComplexObject()).close();
    assertEquals(getContents(root, "complex.txt"), sw.toString());
  }

  public void testSerialCallable() throws MustacheException, IOException {
    MustacheFactory c = init();
    Mustache m = c.compile("complex.html");
    StringWriter sw = new StringWriter();
    m.execute(sw, new ParallelComplexObject());
    assertEquals(getContents(root, "complex.txt"), sw.toString());
  }

  /*
  @SuppressWarnings("serial")
  public void testCurrentElementInArray() throws IOException, MustacheException {

      MustacheBuilder c = init();
      Mustache m = c.parseFile("simple_array.html");
      StringWriter sw = new StringWriter();
      FutureWriter writer = new FutureWriter(sw);
      m.execute(writer, new Scope(new HashMap<String, Object>() {
          {
              put("list", Arrays.asList(1,2,3));
          }
      }));
      writer.flush();
      assertEquals(getContents(root, "simple_array.txt"), sw.toString());

      sw = new StringWriter();
      writer = new FutureWriter(sw);
      m.execute(writer, new Scope(new HashMap<String, Object>() {
          {
              put("list", Arrays.asList(null,null));
          }
      }));
      writer.flush();
      assertEquals("\n\n", sw.toString());

  }
  */
  public void testReadme() throws MustacheException, IOException {
    MustacheFactory c = init();
    Mustache m = c.compile("items.html");
    StringWriter sw = new StringWriter();
    long start = System.currentTimeMillis();
    m.execute(sw, new Context());
    long diff = System.currentTimeMillis() - start;
    assertEquals(getContents(root, "items.txt"), sw.toString());
  }

  public void testReadmeSerial() throws MustacheException, IOException {
    MustacheFactory c = init();
    Mustache m = c.compile("items2.html");
    StringWriter sw = new StringWriter();
    long start = System.currentTimeMillis();
    m.execute(sw, new Context());
    long diff = System.currentTimeMillis() - start;
    assertEquals(getContents(root, "items.txt"), sw.toString());
    assertTrue("Should be a little bit more than 4 seconds: " + diff, diff > 3999 && diff < 6000);
  }

  public void testReadmeParallel() throws MustacheException, IOException {
    MustacheFactory c = initParallel();
    Mustache m = c.compile("items2.html");
    StringWriter sw = new StringWriter();
    long start = System.currentTimeMillis();
    m.execute(sw, new Context()).close();
    long diff = System.currentTimeMillis() - start;
    assertEquals(getContents(root, "items.txt"), sw.toString());
    assertTrue("Should be a little bit more than 1 second: " + diff, diff > 999 && diff < 2000);
  }

  static class Context {
    List<Item> items() {
      return Arrays.asList(
              new Item("Item 1", "$19.99",
                      Arrays.asList(new Feature("New!"), new Feature("Awesome!"))),
              new Item("Item 2", "$29.99", Arrays.asList(new Feature("Old."), new Feature("Ugly.")))
      );
    }

    static class Item {
      Item(String name, String price, List<Feature> features) {
        this.name = name;
        this.price = price;
        this.features = features;
      }

      String name, price;
      List<Feature> features;
    }

    static class Feature {
      Feature(String description) {
        this.description = description;
      }

      String description;

      Callable<String> desc() throws InterruptedException {
        return new Callable<String>() {
          @Override
          public String call() throws Exception {
            Thread.sleep(1000);
            return description;
          }
        };
      }
    }
  }

  public void testDeferred() throws IOException {
    DefaultMustacheFactory mf = new DeferringMustacheFactory(root);
    mf.setExecutorService(Executors.newCachedThreadPool());
    Object context = new Object() {
      String title = "Deferred";
      Object deferred = new DeferringMustacheFactory.DeferredCallable();
      Object deferredpartial = DeferringMustacheFactory.DEFERRED;
    };
    Mustache m = mf.compile("deferred.html");
    StringWriter sw = new StringWriter();
    m.execute(sw, context).close();
    assertEquals(getContents(root, "deferred.txt"), sw.toString());
  }

  public void testMultipleCallsWithDifferentScopes() throws IOException {
    String template = "Value: {{value}}";
    Mustache mustache = new DefaultMustacheFactory().compile(new StringReader(
            template), "test");

    // scope object doesn't have a 'value' property, lookup will fail
    mustache.execute(new StringWriter(), new Object());

    // scope object has a 'value' property, lookup shouldn't fail
    StringWriter sw = new StringWriter();
    mustache.execute(sw, new Object() {
      String value = "something";
    });

    assertEquals("Value: something", sw.toString());
  }

  public void testMultipleCallsWithDifferentMapScopes() throws IOException {
    String template = "Value: {{value}}";
    Mustache mustache = new DefaultMustacheFactory().compile(new StringReader(
            template), "test");
    Map<String, String> emptyMap = new HashMap<String, String>();
    Map<String, String> map = new HashMap<String, String>();
    map.put("value", "something");

    // map doesn't have an entry for 'value', lookup will fail
    mustache.execute(new StringWriter(), emptyMap);

    // map has an entry for 'value', lookup shouldn't fail
    StringWriter sw = new StringWriter();
    mustache.execute(sw, map);

    assertEquals("Value: something", sw.toString());
  }

  private MustacheFactory init() {
    DefaultMustacheFactory cf = createMustacheFactory();
    return cf;
  }

  private DefaultMustacheFactory initParallel() {
    DefaultMustacheFactory cf = createMustacheFactory();
    cf.setExecutorService(Executors.newCachedThreadPool());
    return cf;
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

  protected void setUp() throws Exception {
    super.setUp();
    File file = new File("src/test/resources");
    root = new File(file, "simple.html").exists() ? file : new File("../src/test/resources");
  }

}
