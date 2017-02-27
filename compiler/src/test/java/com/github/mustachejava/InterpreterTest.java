package com.github.mustachejava;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.MappingJsonFactory;
import com.github.mustachejava.codes.IterableCode;
import com.github.mustachejava.codes.PartialCode;
import com.github.mustachejava.codes.ValueCode;
import com.github.mustachejava.codes.WriteCode;
import com.github.mustachejava.functions.CommentFunction;
import com.github.mustachejava.reflect.Guard;
import com.github.mustachejava.reflect.GuardedBinding;
import com.github.mustachejava.reflect.MissingWrapper;
import com.github.mustachejava.reflect.ReflectionObjectHandler;
import com.github.mustachejava.reflect.SimpleObjectHandler;
import com.github.mustachejava.resolver.DefaultResolver;
import com.github.mustachejava.util.CapturingMustacheVisitor;
import com.github.mustachejavabenchmarks.JsonCapturer;
import com.github.mustachejavabenchmarks.JsonInterpreterTest;
import junit.framework.TestCase;
import org.junit.Assert;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;

import static com.github.mustachejava.TestUtil.getContents;
import static java.util.Collections.singletonList;

/**
 * Tests for the compiler.
 * <p/>
 * User: sam
 * Date: May 3, 2010
 * Time: 10:23:54 AM
 */
@SuppressWarnings("unused")
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

  private static class LocalizedMustacheResolver extends DefaultResolver {
    private final Locale locale;

    LocalizedMustacheResolver(File root, Locale locale) {
      super(root);
      this.locale = locale;
    }

    @Override
    public Reader getReader(String resourceName) {

      // Build resource name with locale suffix
      int index = resourceName.lastIndexOf('.');
      String newResourceName;
      if (index == -1) {
        newResourceName = resourceName;
      } else {
        newResourceName = resourceName.substring(0, index) + "_" + locale.toLanguageTag() + resourceName.substring(index);
      }

      // First look with locale
      Reader reader = super.getReader(newResourceName);

      if (reader == null) {
        // Fallback to non-localized resourceName
        reader = super.getReader(resourceName);
      }

      return reader;
    }
  }

  public void testSimpleI18N() throws MustacheException, IOException, ExecutionException, InterruptedException {
    {
      MustacheFactory c = new DefaultMustacheFactory(new LocalizedMustacheResolver(root, Locale.KOREAN));
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
      assertEquals(getContents(root, "simple_ko.txt"), sw.toString());
    }
    {
      MustacheFactory c = new DefaultMustacheFactory(new LocalizedMustacheResolver(root, Locale.JAPANESE));
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

  private DefaultMustacheFactory createMustacheFactory() {
    return new DefaultMustacheFactory(root);
  }

  public void testRecurision() throws IOException {
    StringWriter sw = execute("recursion.html", new Object() {
      Object value = new Object() {
        boolean value = false;
      };
    });
    assertEquals(getContents(root, "recursion.txt"), sw.toString());
  }

  public void testRecursionWithInheritance() throws IOException {
    StringWriter sw = execute("recursion_with_inheritance.html", new Object() {
      Object value = new Object() {
        boolean value = false;
      };
    });
    assertEquals(getContents(root, "recursion.txt"), sw.toString());
  }

  public void testPartialRecursionWithInheritance() throws IOException {
    StringWriter sw = execute("recursive_partial_inheritance.html", new Object() {
      Object test = new Object() {
        boolean test = false;
      };
    });
    assertEquals(getContents(root, "recursive_partial_inheritance.txt"), sw.toString());
  }

  public void testChainedInheritance() throws IOException {
    StringWriter sw = execute("page.html", new Object() {
      Object test = new Object() {
        boolean test = false;
      };
    });
    assertEquals(getContents(root, "page.txt"), sw.toString());
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
    final Map<String, Object> params = new HashMap<>();
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
              () -> {
                cdl1.await();
                sb.append("How");
                return "How";
              },
              (Callable<Object>) () -> {
                cdl2.await();
                sb.append("are");
                cdl1.countDown();
                return "are";
              },
              () -> {
                sb.append("you?");
                cdl2.countDown();
                return "you?";
              }
      );
    }).close();
    assertEquals(getContents(root, "latchedtest.txt"), sw.toString());
    assertEquals("you?areHow", sb.toString());
  }

  public void testConcurrency() throws IOException {
    DefaultMustacheFactory c = createMustacheFactory();
    c.setExecutorService(Executors.newCachedThreadPool());
    Mustache m = c.compile(new StringReader("{{a}} {{#caps}}{{b}}{{/caps}} {{c}}"), "concurrency");
    StringWriter sw = new StringWriter();
    long start = System.currentTimeMillis();
    Writer execute = m.execute(sw, new Object() {
      Callable<Object> a = () -> {
        Thread.sleep(300);
        return "How";
      };
      Callable<Object> b = () -> {
        Thread.sleep(200);
        return "are";
      };
      Callable<Object> c = () -> {
        Thread.sleep(100);
        return "you?";
      };
      Callable<Function> caps = () -> (Function) o -> o.toString().toUpperCase();
    });
    execute.close();
    assertTrue("Time < 600ms", System.currentTimeMillis() - start < 600);
    assertEquals("How ARE you?", sw.toString());
  }

  public void testNestedLatches() throws IOException {
    DefaultMustacheFactory c = createMustacheFactory();
    c.setExecutorService(Executors.newCachedThreadPool());
    Mustache m = c.compile("latchedtest.html");
    StringWriter sw = new StringWriter();
    Writer execute = m.execute(sw, new Object() {
      Callable<Object> nest = () -> {
        Thread.sleep(300);
        return "How";
      };
      Callable<Object> nested = () -> {
        Thread.sleep(200);
        return "are";
      };
      Callable<Object> nestest = () -> {
        Thread.sleep(100);
        return "you?";
      };
    });
    execute.close();

    assertEquals("<outer>\n<inner>How</inner>\n<inner>are</inner>\n<inner>you?</inner>\n</outer>\n", sw.toString());
  }

  public void testBrokenSimple() throws MustacheException, IOException, ExecutionException, InterruptedException {
    try {
      MustacheFactory c = createMustacheFactory();
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
    Object object = new Object() {
      List<String> people = singletonList("Test");
    };
    StringWriter sw = execute("isempty.html", object);
    assertEquals(getContents(root, "isempty.txt"), sw.toString());
  }

  private StringWriter execute(String name, Object object) {
    MustacheFactory c = createMustacheFactory();
    Mustache m = c.compile(name);
    StringWriter sw = new StringWriter();
    m.execute(sw, object);
    return sw;
  }

  private StringWriter execute(String name, List<Object> objects) {
    MustacheFactory c = createMustacheFactory();
    Mustache m = c.compile(name);
    StringWriter sw = new StringWriter();
    m.execute(sw, objects);
    return sw;
  }

  public void testImmutableList() throws IOException {
    Object object = new Object() {
      List<String> people = singletonList("Test");
    };
    StringWriter sw = execute("isempty.html", singletonList(object));
    assertEquals(getContents(root, "isempty.txt"), sw.toString());
  }

  public void testOptional() throws IOException {
    MustacheFactory c = createMustacheFactory();
    StringReader template = new StringReader("{{person}}{{#person}} is present{{/person}}{{^person}}Is not present{{/person}}");
    Mustache m = c.compile(template, "test");
    StringWriter sw = new StringWriter();
    m.execute(sw, new Object() {
      Optional<String> person = Optional.of("Test");
    });
    assertEquals("Test is present", sw.toString());
    sw = new StringWriter();
    m.execute(sw, new Object() {
      Optional<String> person = Optional.empty();
    });
    assertEquals("Is not present", sw.toString());
  }

  public void testComment() throws IOException {
    MustacheFactory c = createMustacheFactory();
    Mustache m = c.compile(new StringReader("{{#process}}{{!comment}}{{/process}}"), "");
    StringWriter sw = new StringWriter();
    m.execute(sw, new Object() {
      TemplateFunction process = s -> s.replace("{", "[");
    });
    assertEquals("[[!comment}}", sw.toString());
  }

  public void testNumber0IsFalse() throws IOException {
    DefaultMustacheFactory c = createMustacheFactory();
    c.setObjectHandler(new ReflectionObjectHandler() {
      @Override
      public Writer falsey(Iteration iteration, Writer writer, Object object, List<Object> scopes) {
        if (object instanceof Number) {
          if (((Number) object).intValue() == 0) {
            return iteration.next(writer, object, scopes);
          }
        }
        return super.falsey(iteration, writer, object, scopes);
      }

      @Override
      public Writer iterate(Iteration iteration, Writer writer, Object object, List<Object> scopes) {
        if (object instanceof Number) {
          if (((Number) object).intValue() == 0) {
            return writer;
          }
        }
        return super.iterate(iteration, writer, object, scopes);
      }
    });
    StringWriter sw = new StringWriter();
    Mustache m = c.compile(new StringReader("{{#zero}}zero{{/zero}}{{#one}}one{{/one}}{{^zero}}zero{{/zero}}{{^one}}one{{/one}}"), "zeroone");
    m.execute(sw, new Object() {
      int zero = 0;
      int one = 1;
    }).close();
    assertEquals("onezero", sw.toString());
  }

  public void testSecurity() throws MustacheException, IOException, ExecutionException, InterruptedException {
    MustacheFactory c = createMustacheFactory();
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
    MustacheFactory c = createMustacheFactory();
    Mustache m = c.compile("simple.html");
    StringWriter sw = new StringWriter();
    m.identity(sw);
    assertEquals(getContents(root, "simple.html").replaceAll("\\s+", ""), sw.toString().replaceAll(
            "\\s+", ""));
  }

  public void testProperties() throws MustacheException, IOException, ExecutionException, InterruptedException {
    MustacheFactory c = createMustacheFactory();
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
    StringWriter sw = execute("simple.html", new HashMap<String, Object>() {{
      put("name", "Chris");
      put("value", 10000);
      put("taxed_value", 6000);
      put("in_ca", true);
    }});
    assertEquals(getContents(root, "simple.txt"), sw.toString());
  }

  public void testPartialWithTF() throws MustacheException, IOException {
    MustacheFactory c = createMustacheFactory();
    Mustache m = c.compile("partialintemplatefunction.html");
    StringWriter sw = new StringWriter();
    m.execute(sw, new Object() {
      public TemplateFunction i() {
        return s -> s;
      }
    });
    assertEquals("This is not interesting.", sw.toString());
  }

  public void testFunctions() throws IOException {
    MustacheFactory c = createMustacheFactory();
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
    m = createMustacheFactory().compile("complex.html");
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
    StringWriter sw = execute("complex.html", new ParallelComplexObject());
    assertEquals(getContents(root, "complex.txt"), sw.toString());
  }

  public void testDynamicPartial() throws MustacheException, IOException {
    // Implement >+ syntax that dynamically includes template files at runtime
    MustacheFactory c = new DefaultMustacheFactory(root) {
      @Override
      public MustacheVisitor createMustacheVisitor() {
        return new DefaultMustacheVisitor(this) {
          @Override
          public void partial(TemplateContext tc, String variable) {
            if (variable.startsWith("+")) {
              // This is a dynamic partial rather than a static one
              TemplateContext partialTC = new TemplateContext("{{", "}}", tc.file(), tc.line(), tc.startOfLine());
              list.add(new PartialCode(partialTC, df, variable.substring(1).trim()) {
                @Override
                public synchronized void init() {
                  filterText();
                  // Treat the actual text as a Mustache with single [ ] as the delimiter
                  // so we can do interpoliation of things like [foo]/[bar].txt
                  partial = df.compile(new StringReader(name), "__dynpartial__", "[", "]");
                  if (partial == null) {
                    throw new MustacheException("Failed to parse partial name template: " + name);
                  }
                }

                ConcurrentMap<String, Mustache> dynamicaPartialCache = new ConcurrentHashMap<>();

                @Override
                public Writer execute(Writer writer, List<Object> scopes) {
                  // Calculate the name of the dynamic partial
                  StringWriter sw = new StringWriter();
                  partial.execute(sw, scopes);
                  Mustache mustache = dynamicaPartialCache.computeIfAbsent(sw.toString(), df::compilePartial);
                  Writer execute = mustache.execute(writer, scopes);
                  return appendText(execute);
                }
              });
            } else {
              super.partial(tc, variable);
            }
          }
        };
      }
    };
    Mustache m = c.compile(new StringReader("{{>+ [foo].html}}"), "test.html");
    StringWriter sw = new StringWriter();
    m.execute(sw, new HashMap<String, Object>() {{
      put("name", "Chris");
      put("value", 10000);
      put("taxed_value", 6000);
      put("in_ca", true);
      put("foo", "simple");
    }});
    assertEquals(getContents(root, "simple.txt"), sw.toString());
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
    MustacheFactory c = createMustacheFactory();
    Mustache m = c.compile("items.html");
    StringWriter sw = new StringWriter();
    long start = System.currentTimeMillis();
    m.execute(sw, new Context());
    long diff = System.currentTimeMillis() - start;
    assertEquals(getContents(root, "items.txt"), sw.toString());
  }

  public void testReadmeSerial() throws MustacheException, IOException {
    MustacheFactory c = createMustacheFactory();
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
        return () -> {
          Thread.sleep(1000);
          return description;
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
    Map<String, String> emptyMap = new HashMap<>();
    Map<String, String> map = new HashMap<>();
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
    compile.execute(sw, "").close();
    assertEquals(getContents(root, "relative/paths.txt"), sw.toString());
  }

  public void testRelativePathsRootDir() throws IOException {
    MustacheFactory mf = createMustacheFactory();
    Mustache compile = mf.compile("relative/rootpath.html");
    StringWriter sw = new StringWriter();
    compile.execute(sw, "").close();
    assertEquals(getContents(root, "relative/paths.txt"), sw.toString());
  }

  public void testPathsWithExtension() throws IOException {
    MustacheFactory mf = createMustacheFactory();
    Mustache compile = mf.compile("relative/extension.html");
    StringWriter sw = new StringWriter();
    compile.execute(sw, "").close();
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
    m.execute(sw, new HashMap<String, String>() {{
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
        visitor.addPragmaHandler("pragma", (tc, pragma, args) -> {
          if (pragma.equals("pragma") && args.equals("1 2 3")) {
            found.set(true);
          }
          return null;
        });
        return visitor;
      }
    };
    Mustache m = mf.compile(new StringReader("Pragma: {{% pragma 1 2 3 }}"), "testPragma");
    StringWriter sw = new StringWriter();
    m.execute(sw, "").close();
    // Values ignored as if it didn't exist at all
    assertEquals("Pragma: ", sw.toString());
    assertTrue(found.get());
  }

  public void testPragmaWhiteSpaceHandling() throws IOException {
    DefaultMustacheFactory mf = new DefaultMustacheFactory() {
      @Override
      public MustacheVisitor createMustacheVisitor() {
        DefaultMustacheVisitor visitor = new DefaultMustacheVisitor(this);
        // Add a pragma handler that simply renders a dash to visualize the output
        visitor.addPragmaHandler("pragma", (tc, pragma, args) -> new WriteCode(tc, this, "-"));
        return visitor;
      }
    };

    Mustache m = mf.compile(new StringReader(" {{% pragma}} {{% pragma}} "), "testPragma");
    StringWriter sw = new StringWriter();
    m.execute(sw, "").close();

    // Pragma rendering should preserve template whitespace
    assertEquals(" - - ", sw.toString());
  }

  public void testNotIterableCallable() throws IOException {
    MustacheFactory mf = createMustacheFactory();
    Mustache m = mf.compile(new StringReader("{{^value}}test{{/value}}"), "testNotIterableCallable");
    StringWriter sw = new StringWriter();
    m.execute(sw, new Object() {
      Callable value = () -> null;
    }).close();
    // Values ignored as if it didn't exist at all
    assertEquals("test", sw.toString());
  }

  private static class AccessTrackingMap extends HashMap<String, String> {
    Set<String> accessed = new HashSet<>();

    @Override
    public String get(Object key) {
      accessed.add((String) key);
      return super.get(key);
    }

    void check() {
      Set<String> keyset = new HashSet<>(keySet());
      keyset.removeAll(accessed);
      if (!keyset.isEmpty()) {
        throw new MustacheException("All keys in the map were not accessed");
      }
    }
  }

  public void testAccessTracker() throws IOException {
    {
      Map<String, String> accessTrackingMap = createBaseMap();
      DefaultMustacheFactory mf = createMustacheFactory();
      Mustache test = mf.compile(new StringReader("{{first}} {{last}}"), "test");
      StringWriter sw = new StringWriter();
      test.execute(sw, accessTrackingMap).close();
      assertEquals("Sam Pullara", sw.toString());
    }
    {
      AccessTrackingMap accessTrackingMap = createBaseMap();
      accessTrackingMap.put("notused", "shouldcauseanerror");
      DefaultMustacheFactory mf = createMustacheFactory();
      Mustache test = mf.compile(new StringReader("{{first}} {{last}}"), "test");
      StringWriter sw = new StringWriter();
      test.execute(sw, accessTrackingMap).close();
      assertEquals("Sam Pullara", sw.toString());
      try {
        accessTrackingMap.check();
        fail("Should have thrown an exception");
      } catch (MustacheException me) {
        // Succcess
      }
    }
  }

  private AccessTrackingMap createBaseMap() {
    AccessTrackingMap accessTrackingMap = new AccessTrackingMap();
    accessTrackingMap.put("first", "Sam");
    accessTrackingMap.put("last", "Pullara");
    return accessTrackingMap;
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
      Function f = o -> null;
      CommentFunction comment = new CommentFunction();
    }).close();
    // Values ignored as if it didn't exist at all
    assertEquals("test", sw.toString());
  }

  private static class SuperClass {
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
    compile.execute(sw, "").close();
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
    compile.execute(sw, "").close();
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
    mf.compile("overrideextension.html").execute(sw, "").close();
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

  public void testMustacheNotFoundException() {
    String nonExistingMustache = "404";
    try {
      new DefaultMustacheFactory().compile(nonExistingMustache);
      fail("Didn't throw an exception");
    } catch (MustacheNotFoundException e) {
      assertEquals(nonExistingMustache, e.getName());
    }
  }

  public void testImplicitIteratorNoScope() throws IOException {
    Mustache test = new DefaultMustacheFactory().compile(new StringReader("{{.}}"), "test");
    StringWriter sw = new StringWriter();
    test.execute(sw, "").close();
    assertEquals("", sw.toString());
    StringWriter sw2 = new StringWriter();
    test.execute(sw2, new Object[0]).close();
    assertEquals("", sw2.toString());
  }

  public void testImplicitIteratorWithScope() throws IOException {
    Mustache test = new DefaultMustacheFactory().compile(new StringReader("{{#test}}_{{.}}_{{/test}}"), "test");
    StringWriter sw = new StringWriter();
    test.execute(sw, new Object() {
      List<String> test = Arrays.asList("a", "b", "c");
    }).close();
    assertEquals("_a__b__c_", sw.toString());
  }

  public void testCR() {
    Mustache m = new DefaultMustacheFactory().compile(new StringReader("{{test}}\r\n{{test}}\r\n"), "test");
    StringWriter sw = new StringWriter();
    m.execute(sw, new Object() {
      String test = "fred";
    });
    assertEquals("fred\r\nfred\r\n", sw.toString());
  }

  public void testOutputDelimiters() {
    String template = "{{=## ##=}}{{##={{ }}=####";
    Mustache mustache = new DefaultMustacheFactory().compile(new StringReader(template), "test");
    StringWriter sw = new StringWriter();
    mustache.execute(sw, new Object[0]);
    assertEquals("{{##", sw.toString());
  }

  public void testImproperlyClosedVariable() throws IOException {
    try {
      new DefaultMustacheFactory().compile(new StringReader("{{{#containers}} {{/containers}}"), "example");
      fail("Should have throw MustacheException");
    } catch (MustacheException actual) {
      assertEquals("Improperly closed variable in example:1 @[example:1]", actual.getMessage());
    }
  }

  public void testLimitedDepthRecursion() {
    try {
      StringWriter sw = execute("infiniteparent.html", new Context());
      fail("Should have failed");
    } catch (StackOverflowError soe) {
      fail("Should not have overflowed the stack");
    } catch (MustacheException e) {
      assertEquals("Maximum partial recursion limit reached: 100 @[infiniteparent.html:1]", e.getMessage());
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
      assertEquals("Failed to close 'value' tag @[test:2]", e.getMessage());
    }
  }

  public void testTemplateFunctionWithData() {
    String template = "{{#parse}}\n" +
            "{{replaceMe}}\n" +
            "{{/parse}}";
    Mustache mustache = new DefaultMustacheFactory().compile(new StringReader(template), "test");
    StringWriter sw = new StringWriter();
    mustache.execute(sw, new Object() {
      public TemplateFunction parse() {
        return s -> "blablabla {{anotherVar}}, blablabla {{yetAnotherVar}}";
      }

      String anotherVar = "banana";
      String yetAnotherVar = "apple";
    });
    assertEquals("blablabla banana, blablabla apple", sw.toString());
  }

  public void testTemplateFunctionWithImplicitParams() {
    String template = "{{#parse}}\n" +
            "{{replaceMe}}\n" +
            "{{/parse}}";
    DefaultMustacheFactory mf = new DefaultMustacheFactory() {
      public MustacheVisitor createMustacheVisitor() {
        return new DefaultMustacheVisitor(this) {
          public void iterable(final TemplateContext templateContext, String variable, Mustache mustache) {
            list.add(new IterableCode(templateContext, df, mustache, variable) {
              Binding binding = oh.createBinding("params", templateContext, this);

              protected Writer handleFunction(Writer writer, Function function, List<Object> scopes) {
                boolean added = addScope(scopes, binding.get(scopes));
                try {
                  return super.handleFunction(writer, function, scopes);
                } finally {
                  if (added) scopes.remove(scopes.size() - 1);
                }
              }
            });
          }
        };
      }
    };
    Mustache mustache = mf.compile(new StringReader(template), "test");
    StringWriter sw = new StringWriter();
    mustache.execute(sw, new Object() {
      public TemplateFunction parse() {
        return s -> "blablabla {{anotherVar}}, blablabla {{yetAnotherVar}}";
      }

      Map<String, Object> params = new HashMap<>();

      {
        params.put("anotherVar", "banana");
        params.put("yetAnotherVar", "apple");
      }
    });
    assertEquals("blablabla banana, blablabla apple", sw.toString());
  }

  public void testOverrideValueCode() throws IOException {
    DefaultMustacheFactory mf = new DefaultMustacheFactory() {
      @Override
      public MustacheVisitor createMustacheVisitor() {
        return new DefaultMustacheVisitor(this) {
          @Override
          public void value(TemplateContext tc, String variable, boolean encoded) {
            list.add(new ValueCode(tc, df, variable, encoded) {
              @Override
              public Writer execute(Writer writer, List<Object> scopes) {
                try {
                  final Object object = get(scopes);
                  if (object != null) {
                    if (object instanceof Function) {
                      handleFunction(writer, (Function) object, scopes);
                    } else if (object instanceof Callable) {
                      return handleCallable(writer, (Callable) object, scopes);
                    } else {
                      String stringify = oh.stringify(object);
                      if (stringify.equals("")) {
                        GuardedBinding.logWarning("Variable is empty string: ", variable, scopes, tc);
                      }
                      execute(writer, stringify);
                    }
                  } else {
                    GuardedBinding.logWarning("Variable is null: ", variable, scopes, tc);
                  }
                  return appendText(run(writer, scopes));
                } catch (Exception e) {
                  throw new MustacheException("Failed to get value for " + name, e, tc);
                }
              }
            });
          }
        };
      }
    };
    mf.setObjectHandler(new ReflectionObjectHandler() {
      @Override
      protected MissingWrapper createMissingWrapper(String name, List<Guard> guards) {
        throw new MustacheException("Failed to find: " + name);
      }
    });
    StringReader sr;
    StringWriter sw;
    {
      sw = new StringWriter();
      sr = new StringReader("{{value}}");
      Mustache m = mf.compile(sr, "value");
      try {
        m.execute(sw, new Object() {
        });
        fail("Should throw an exception");
      } catch (MustacheException e) {
      }
    }
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    PrintStream ps = new PrintStream(out);
    System.setErr(ps);
    {
      sw = new StringWriter();
      sr = new StringReader("{{value}}");
      Mustache m = mf.compile(sr, "value");
      try {
        m.execute(sw, new Object() {
          String value = null;
        }).close();
        ps.flush();
        assertTrue(new String(out.toByteArray()).contains("Variable is null"));
      } catch (MustacheException e) {
      }
    }
    out.reset();
    {
      sw = new StringWriter();
      sr = new StringReader("{{value}}");
      Mustache m = mf.compile(sr, "value");
      try {
        m.execute(sw, new Object() {
          String value = "";
        }).close();
        ps.flush();
        assertTrue(new String(out.toByteArray()).contains("Variable is empty string"));
      } catch (MustacheException e) {
      }
    }
    out.reset();
    {
      sw = new StringWriter();
      sr = new StringReader("{{value}}");
      Mustache m = mf.compile(sr, "value");
      try {
        m.execute(sw, new Object() {
          String value = "value";
        }).close();
        ps.flush();
        assertEquals("", new String(out.toByteArray()));
        assertEquals("value", sw.toString());
      } catch (MustacheException e) {
      }
    }
  }

  public void testPropertyWithDot() throws IOException {
    DefaultMustacheFactory mustacheFactory = new DefaultMustacheFactory();
    Reader reader = new StringReader("value=${some.value}");
    Mustache mustache = mustacheFactory.compile(reader, "maven", "${", "}");
    Map<String, String> properties = new HashMap<>();
    properties.put("some.value", "some.value");
    StringWriter writer = new StringWriter();
    mustache.execute(writer, new Object[]{properties}).close();
    Assert.assertEquals("value=some.value", writer.toString());
  }

  public void testLeavingAloneMissingVariables() throws IOException {
    DefaultMustacheFactory dmf = new DefaultMustacheFactory(root) {
      @Override
      public MustacheVisitor createMustacheVisitor() {
        return new DefaultMustacheVisitor(this) {
          @Override
          public void value(TemplateContext tc, String variable, boolean encoded) {
            list.add(new ValueCode(tc, df, variable, encoded) {
              @Override
              public Writer execute(Writer writer, List<Object> scopes) {
                try {
                  final Object object = get(scopes);
                  if (object == null) {
                    identity(writer);
                  }
                  return super.execute(writer, scopes);
                } catch (Exception e) {
                  throw new MustacheException("Failed to get value for " + name, e, tc);
                }
              }
            });
          }
        };
      }
    };
    Mustache test = dmf.compile(new StringReader("{{name}} - {{email}}"), "test");
    StringWriter sw = new StringWriter();
    Map<Object, Object> map = new HashMap<>();
    map.put("name", "Sam Pullara");
    test.execute(sw, map).close();
    assertEquals("Sam Pullara - {{email}}", sw.toString());
  }

  private DefaultMustacheFactory initParallel() {
    DefaultMustacheFactory cf = createMustacheFactory();
    cf.setExecutorService(Executors.newCachedThreadPool());
    return cf;
  }

  protected void setUp() throws Exception {
    super.setUp();
    File file = new File("src/test/resources");
    root = new File(file, "simple.html").exists() ? file : new File("../src/test/resources");
  }

  @Test
  public void testMap() throws IOException {
    ArrayList<Map<String, String>> fn = new ArrayList<>();
    Map<String, String> map1 = new HashMap<>();
    map1.put("name", "firstName");
    map1.put("last", "lastName");
    fn.add(map1);
    Map<String, String> map2 = new HashMap<>();
    map2.put("name", "firstName 1");
    map2.put("last", "lastName 1");
    fn.add(map2);
    Map<String, ArrayList<Map<String, String>>> map = new HashMap<>();
    map.put("names", fn);

    Writer writer = new OutputStreamWriter(System.out);
    MustacheFactory mf = new DefaultMustacheFactory();
    Mustache mustache = mf.compile(new StringReader("{{#names}}<h2>First Name : {{name}}</h2> <h2>Last Name : {{last}}</h2>{{/names}}"), "example");
    mustache.execute(writer, map);
    writer.flush();
  }
}
