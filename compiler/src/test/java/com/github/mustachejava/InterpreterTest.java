package com.github.mustachejava;

import com.github.mustachejava.codes.PartialCode;
import com.github.mustachejava.functions.CommentFunction;
import com.github.mustachejava.reflect.SimpleObjectHandler;
import com.github.mustachejava.util.CapturingMustacheVisitor;
import com.github.mustachejavabenchmarks.JsonCapturer;
import com.github.mustachejavabenchmarks.JsonInterpreterTest;
import com.google.common.base.Function;
import junit.framework.TestCase;
import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.MappingJsonFactory;

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
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Tests for the compiler.
 * <p/>
 * User: sam
 * Date: May 3, 2010
 * Time: 10:23:54 AM
 */
public class InterpreterTest extends TestCase {
  protected File root;

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

  public void testRootCheck() throws MustacheException, IOException, ExecutionException, InterruptedException {
    MustacheFactory c = createMustacheFactory();
    try {
      Mustache m = c.compile("../../../pom.xml");
      fail("Should have failed to compile");
    } catch (MustacheException e) {
      // Success
    }
  }

  public void testSimpleFiltered() throws MustacheException, IOException, ExecutionException, InterruptedException {
    MustacheFactory c = new DefaultMustacheFactory(root) {
      /**
       * Override this method to apply any filtering to text that will appear
       * verbatim in the output template.
       *
       *
       *
       * @param appended
       * @param startOfLine
       * @return
       */
      @Override
      public String filterText(String appended, boolean startOfLine) {
        // Remove duplicate spaces, leading spaces and trailing spaces
        if (startOfLine) {
          appended = appended.replaceAll("^[\t ]+", "");
        }
        return appended
                .replaceAll("[ \t]+", " ")
                .replaceAll("[ \n\t]*\n[ \n\t]*", "\n");
      }
    };
    Mustache m = c.compile("simplefiltered.html");
    StringWriter sw = new StringWriter();
    m.execute(sw, new Object() {
      String name = "Chris";
      int value = 10000;

      int taxed_value() {
        return (int) (this.value - (this.value * 0.4));
      }

      boolean in_ca = true;
    });
    assertEquals(getContents(root, "simplefiltered.txt"), sw.toString());
  }

  public void testTypedSimple() throws MustacheException, IOException, ExecutionException, InterruptedException {
    final Object scope = new Object() {
      String name = "Chris";
      int value = 10000;

      class MyObject {
        int taxed_value() {
          return (int) (value - (value * 0.4));
        }

        String fred = "";
      }

      MyObject in_ca = new MyObject();

      boolean test = false;
    };
    DefaultMustacheFactory c = new DefaultMustacheFactory(root);
    c.setObjectHandler(new TypeCheckingHandler());
    Mustache m = c.compile("simple.html");
    StringWriter sw = new StringWriter();
    m.execute(sw, scope.getClass()).flush();
    assertEquals(getContents(root, "simpletyped.txt"), sw.toString());
  }

  protected DefaultMustacheFactory createMustacheFactory() {
    return new DefaultMustacheFactory(root);
  }

  public void testRecurision() throws IOException {
    MustacheFactory c = createMustacheFactory();
    Mustache m = c.compile("recursion.html");
    StringWriter sw = new StringWriter();
    m.execute(sw, new Object() {
      Object value = new Object() {
        boolean value = false;
      };
    });
    assertEquals(getContents(root, "recursion.txt"), sw.toString());

  }

  public void testRecursionWithInheritance() throws IOException {
    MustacheFactory c = createMustacheFactory();
    Mustache m = c.compile("recursion_with_inheritance.html");
    StringWriter sw = new StringWriter();
    m.execute(sw, new Object() {
      Object value = new Object() {
        boolean value = false;
      };
    });
    assertEquals(getContents(root, "recursion.txt"), sw.toString());
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

  private class OkGenerator {
    public boolean isItOk() {
      return true;
    }
  }

  public void testNestedAccessWithSimpleObjectHandler() throws IOException {
    assertEquals(getOutput(false), getOutput(true));
  }

  private String getOutput(final boolean setObjectHandler) {
    final DefaultMustacheFactory mustacheFactory = new DefaultMustacheFactory();
    if (setObjectHandler) {
      mustacheFactory.setObjectHandler(new SimpleObjectHandler());
    }
    final Mustache defaultMustache = mustacheFactory.compile(new StringReader("{{#okGenerator.isItOk}}{{okGenerator.isItOk}}{{/okGenerator.isItOk}}"), "Test template");
    final Map<String, Object> params = new HashMap<String, Object>();
    params.put("okGenerator", new OkGenerator());
    final Writer writer = new StringWriter();
    defaultMustache.execute(writer, params);
    return writer.toString();

  }

  public void testClosingReader() {
    final AtomicBoolean closed = new AtomicBoolean();
    StringReader reader = new StringReader("{{test") {
      @Override
      public void close() {
        closed.set(true);
      }
    };
    MustacheFactory mf = new DefaultMustacheFactory();
    try {
      mf.compile(reader, "test");
      fail("Should have thrown an exception");
    } catch (MustacheException me) {
      // The reader should be closed now
      assertEquals(true, closed.get());
    }
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

  public void testPartialWithTF() throws MustacheException, IOException {
    MustacheFactory c = init();
    Mustache m = c.compile("partialintemplatefunction.html");
    StringWriter sw = new StringWriter();
    m.execute(sw, new Object() {
      public TemplateFunction i() {
        return new TemplateFunction() {
          @Override
          public String apply(String s) {
            return s;
          }
        };
      }
    });
    assertEquals("This is not interesting.", sw.toString());
  }

  public void testFunctions() throws IOException {
    MustacheFactory c = init();
    Mustache m = c.compile(new StringReader("{{#f}}{{foo}}{{/f}}"), "test");
    {
      StringWriter sw = new StringWriter();
      m.execute(sw, new Object() {
        Function f = new Function<String, String>() {
          @Override
          public String apply(String s) {
            return s.toUpperCase();
          }
        };
        String foo = "bar";
      }).flush();
      assertEquals("BAR", sw.toString());
    }
    {
      StringWriter sw = new StringWriter();
      m.execute(sw, new Object() {
        Function f = new TemplateFunction() {
          @Override
          public String apply(String s) {
            return s.toUpperCase();
          }
        };
        String foo = "bar";
        String FOO = "baz";
      }).flush();
      assertEquals("baz", sw.toString());
    }
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

  public void testRelativePathsSameDir() throws IOException {
    MustacheFactory mf = createMustacheFactory();
    Mustache compile = mf.compile("relative/paths.html");
    StringWriter sw = new StringWriter();
    compile.execute(sw, null).close();
    assertEquals(getContents(root, "relative/paths.txt"), sw.toString());
  }

  public void testRelativePathsRootDir() throws IOException {
    MustacheFactory mf = createMustacheFactory();
    Mustache compile = mf.compile("relative/rootpath.html");
    StringWriter sw = new StringWriter();
    compile.execute(sw, null).close();
    assertEquals(getContents(root, "relative/paths.txt"), sw.toString());
  }

  public void testRelativePathsTemplateFunction() throws IOException {
    MustacheFactory mf = createMustacheFactory();
    Mustache compile = mf.compile("relative/functionpaths.html");
    StringWriter sw = new StringWriter();
    compile.execute(sw, new Object() {
      Function i = new TemplateFunction() {
        @Override
        public String apply(String s) {
          return s;
        }
      };
    }).close();
    assertEquals(getContents(root, "relative/paths.txt"), sw.toString());
  }

  public void testRelativePathFail() throws IOException {
    MustacheFactory mf = createMustacheFactory();
    try {
      Mustache compile = mf.compile("relative/pathfail.html");
      fail("Should have failed to compile");
    } catch (MustacheException e) {
      // Success
    }
  }

  public void testIterator() throws IOException {
    MustacheFactory mf = createMustacheFactory();
    Mustache m = mf.compile(new StringReader("{{#values}}{{.}}{{/values}}{{^values}}Test2{{/values}}"), "testIterator");
    StringWriter sw = new StringWriter();
    m.execute(sw, new Object() {
      Iterator values() {
        return Arrays.asList(1, 2, 3).iterator();
      }
    }).close();
    assertEquals("123", sw.toString());
  }

  public void testObjectArray() throws IOException {
    MustacheFactory mf = createMustacheFactory();
    Mustache m = mf.compile(new StringReader("{{#values}}{{.}}{{/values}}{{^values}}Test2{{/values}}"), "testObjectArray");
    StringWriter sw = new StringWriter();
    m.execute(sw, new Object() {
      Integer[] values = new Integer[]{1, 2, 3};
    }).close();
    assertEquals("123", sw.toString());
  }

  public void testBaseArray() throws IOException {
    MustacheFactory mf = createMustacheFactory();
    Mustache m = mf.compile(new StringReader("{{#values}}{{.}}{{/values}}{{^values}}Test2{{/values}}"), "testBaseArray");
    StringWriter sw = new StringWriter();
    m.execute(sw, new Object() {
      int[] values = new int[]{1, 2, 3};
    }).close();
    assertEquals("123", sw.toString());
  }

  public void testEmptyString() throws IOException {
    MustacheFactory mf = createMustacheFactory();
    Mustache m = mf.compile(new StringReader("{{#values}}Test1{{/values}}{{^values}}Test2{{/values}}"), "testEmptyString");
    StringWriter sw = new StringWriter();
    m.execute(sw, new Object() {
      String values = "";
    }).close();
    assertEquals("Test2", sw.toString());
  }

  public void testPrivate() throws IOException {
    MustacheFactory mf = createMustacheFactory();
    Mustache m = mf.compile(new StringReader("{{#values}}Test1{{/values}}{{^values}}Test2{{/values}}"), "testPrivate");
    StringWriter sw = new StringWriter();
    m.execute(sw, new Object() {
      private String values = "value";

      private String values() {
        return "value";
      }
    }).close();
    // Values ignored as if it didn't exist at all
    assertEquals("Test2", sw.toString());
  }

  public void testSingleCurly() throws IOException {
    MustacheFactory mf = createMustacheFactory();
    Mustache m = mf.compile(new StringReader("{{value } }}"), "testSingleCurly");
    StringWriter sw = new StringWriter();
    m.execute(sw, new HashMap() {{
      put("value }", "test");
    }}).close();
    // Values ignored as if it didn't exist at all
    assertEquals("test", sw.toString());
  }

  public void testPragma() throws IOException {
    final AtomicBoolean found = new AtomicBoolean();
    DefaultMustacheFactory mf = new DefaultMustacheFactory() {
      @Override
      public MustacheVisitor createMustacheVisitor() {
        DefaultMustacheVisitor visitor = new DefaultMustacheVisitor(this);
        visitor.addPragmaHandler("pragma", new PragmaHandler() {
          @Override
          public Code handle(String pragma, String args) {
            if (pragma.equals("pragma") && args.equals("1 2 3")) {
              found.set(true);
            }
            return null;
          }
        });
        return visitor;
      }
    };
    Mustache m = mf.compile(new StringReader("Pragma: {{% pragma 1 2 3 }}"), "testPragma");
    StringWriter sw = new StringWriter();
    m.execute(sw, null).close();
    // Values ignored as if it didn't exist at all
    assertEquals("Pragma: ", sw.toString());
    assertTrue(found.get());
  }

  public void testNotIterableCallable() throws IOException {
    MustacheFactory mf = createMustacheFactory();
    Mustache m = mf.compile(new StringReader("{{^value}}test{{/value}}"), "testNotIterableCallable");
    StringWriter sw = new StringWriter();
    m.execute(sw, new Object() {
      Callable value = new Callable() {
        @Override
        public Object call() throws Exception {
          return null;
        }
      };
    }).close();
    // Values ignored as if it didn't exist at all
    assertEquals("test", sw.toString());
  }

  public void testMismatch() {
    try {
      MustacheFactory mf = createMustacheFactory();
      Mustache m = mf.compile(new StringReader("{{#value}}"), "testMismatch");
      fail("Not mismatched");
    } catch (MustacheException e) {
      // Success
      try {
        MustacheFactory mf = createMustacheFactory();
        Mustache m = mf.compile(new StringReader("{{#value}}{{/values}}"), "testMismatch");
        fail("Not mismatched");
      } catch (MustacheException e2) {
        // Success
      }
    }
  }

  public void testInvalidDelimiters() {
    try {
      MustacheFactory mf = createMustacheFactory();
      Mustache m = mf.compile(new StringReader("{{=toolong}}"), "testInvalidDelimiters");
      fail("Not invalid");
    } catch (MustacheException e) {
      // Success
    }
  }

  public void testTemplateFunction() throws IOException {
    MustacheFactory mf = createMustacheFactory();
    Mustache m = mf.compile(new StringReader("{{#i}}{{{test}}}{{f}}{{/i}}" +
            "{{#comment}}comment{{/comment}}"), "testTemplateFunction");
    StringWriter sw = new StringWriter();
    m.execute(sw, new Object() {
      Function i = new TemplateFunction() {
        @Override
        public String apply(String s) {
          return s.replace("test", "test2");
        }
      };
      String test2 = "test";
      Function f = new Function() {
        @Override
        public Object apply(Object o) {
          return null;
        }
      };
      CommentFunction comment = new CommentFunction();
    }).close();
    // Values ignored as if it didn't exist at all
    assertEquals("test", sw.toString());
  }

  static class SuperClass {
    String values = "value";
  }

  public void testSuperField() throws IOException {
    MustacheFactory mf = createMustacheFactory();
    Mustache m = mf.compile(new StringReader("{{#values}}Test1{{/values}}{{^values}}Test2{{/values}}"), "testIterator");
    StringWriter sw = new StringWriter();
    m.execute(sw, new SuperClass() {
    }).close();
    // Values ignored as if it didn't exist at all
    assertEquals("Test1", sw.toString());
  }

  public void testRelativePathsDotDotDir() throws IOException {
    MustacheFactory mf = createMustacheFactory();
    Mustache compile = mf.compile("relative/dotdot.html");
    StringWriter sw = new StringWriter();
    compile.execute(sw, null).close();
    assertEquals(getContents(root, "uninterestingpartial.html"), sw.toString());
  }

  public void testRelativePathsDotDotDirOverride() throws IOException {
    MustacheFactory mf = new DefaultMustacheFactory(root) {
      @Override
      public String resolvePartialPath(String dir, String name, String extension) {
        return name + extension;
      }
    };
    Mustache compile = mf.compile("relative/nonrelative.html");
    StringWriter sw = new StringWriter();
    compile.execute(sw, null).close();
    assertEquals(getContents(root, "nonrelative.html"), sw.toString());
  }

  public void testOverrideExtension() throws IOException {
    MustacheFactory mf = new DefaultMustacheFactory(root) {
      @Override
      public MustacheVisitor createMustacheVisitor() {
        return new DefaultMustacheVisitor(this) {
          @Override
          public void partial(TemplateContext tc, String variable) {
            TemplateContext partialTC = new TemplateContext("{{", "}}", tc.file(), tc.line(), tc.startOfLine());
            list.add(new PartialCode(partialTC, df, variable) {
              @Override
              protected String partialName() {
                return name;
              }
            });
          }
        };
      }
    };
    StringWriter sw = new StringWriter();
    mf.compile("overrideextension.html").execute(sw, null).close();
    assertEquals("not interesting.", sw.toString());
  }

  public void testEmptyMustache() {
    try {
      new DefaultMustacheFactory().compile(new StringReader("{{}}"), "test");
      fail("Didn't throw an exception");
    } catch (MustacheException e) {
      assertTrue(e.getMessage().startsWith("Empty mustache"));
    }
  }

  public void testImplicitIteratorNoScope() throws IOException {
    Mustache test = new DefaultMustacheFactory().compile(new StringReader("{{.}}"), "test");
    StringWriter sw = new StringWriter();
    test.execute(sw, null).close();
    assertEquals("", sw.toString());
    StringWriter sw2 = new StringWriter();
    test.execute(sw2, new Object[0]).close();
    assertEquals("", sw2.toString());
  }

  public void testOutputDelimiters() {
    String template = "{{=## ##=}}{{##={{ }}=####";
    Mustache mustache = new DefaultMustacheFactory().compile(new StringReader(template), "test");
    StringWriter sw = new StringWriter();
    mustache.execute(sw, new Object[0]);
    assertEquals("{{##", sw.toString());
  }

  public void testLimitedDepthRecursion() {
    try {
      MustacheFactory c = init();
      Mustache m = c.compile("infiniteparent.html");
      StringWriter sw = new StringWriter();
      m.execute(sw, new Context());
      fail("Should have failed");
    } catch (StackOverflowError soe) {
      fail("Should not have overflowed the stack");
    } catch (MustacheException e) {
      assertEquals("Maximum partial recursion limit reached: 100", e.getMessage());
    }
  }

  public void testMalformedTag() {
    try {
      String template = "\n{{$value}}\n{/value}}";
      Mustache mustache = new DefaultMustacheFactory().compile(new StringReader(template), "test");
      StringWriter sw = new StringWriter();
      mustache.execute(sw, new Object[0]);
      fail("Should have failed to compile");
    } catch (MustacheException e) {
      assertEquals("Failed to close 'value' tag at line 2", e.getMessage());
    }
  }

  private MustacheFactory init() {
    return createMustacheFactory();
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
