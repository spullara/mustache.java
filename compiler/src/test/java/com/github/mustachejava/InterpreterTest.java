package com.github.mustachejava;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.MappingJsonFactory;
import com.github.mustachejava.codes.IterableCode;
import com.github.mustachejava.codes.PartialCode;
import com.github.mustachejava.codes.ValueCode;
import com.github.mustachejava.codes.WriteCode;
import com.github.mustachejava.functions.CommentFunction;
import com.github.mustachejava.reflect.*;
import com.github.mustachejava.resolver.DefaultResolver;
import com.github.mustachejava.util.CapturingMustacheVisitor;
import com.github.mustachejava.util.Wrapper;
import com.github.mustachejavabenchmarks.JsonCapturer;
import com.github.mustachejavabenchmarks.JsonInterpreterTest;
import org.junit.Before;
import org.junit.Test;

import java.io.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;

import static com.github.mustachejava.TestUtil.getContents;
import static java.util.Collections.singletonList;
import static java.util.Collections.singletonMap;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

/**
 * Tests for the compiler.
 * <p/>
 * User: sam
 * Date: May 3, 2010
 * Time: 10:23:54 AM
 */
@SuppressWarnings("unused")
public class InterpreterTest {
  protected File root;

  @Test
  public void testSimple() throws MustacheException, IOException {
    MustacheFactory c = createMustacheFactory();
    Mustache m = c.compile("simple.html");
    StringWriter sw = new StringWriter();
    m.execute(sw, new Object() {
      final String name = "Chris";
      final int value = 10000;

      int taxed_value() {
        return (int) (this.value - (this.value * 0.4));
      }

      final boolean in_ca = true;
    });
    assertEquals(getContents(root, "simple.txt"), sw.toString());
  }

  @Test
  public void testSafeSimple() throws MustacheException, IOException {
    MustacheFactory c = new SafeMustacheFactory(Collections.singleton("simple.html"), root);
    Mustache m = c.compile("simple.html");
    StringWriter sw = new StringWriter();
    m.execute(sw, new Object() {
      public String name = "Chris";
      public int value = 10000;

      public int taxed_value() {
        return (int) (this.value - (this.value * 0.4));
      }

      public boolean in_ca = true;
    });
    assertEquals(getContents(root, "simple.txt"), sw.toString());
  }

  @Test
  public void testSafe() throws MustacheException, IOException {
    assertThrows(MustacheException.class, () -> {
      MustacheFactory c = new SafeMustacheFactory(Collections.singleton("notsimple.html"), root);
      // Not present in allowed list
      c.compile("simple.html");
    });
    MustacheFactory c = new SafeMustacheFactory(Collections.singleton("simple.html"), root);
    {
      Mustache m = c.compile("simple.html");
      StringWriter sw = new StringWriter();
      m.execute(sw, new Object() {
        // It won't find this since it isn't public
        String name = "Chris";
        public int value = 10000;

        public int taxed_value() {
          return (int) (this.value - (this.value * 0.4));
        }

        public boolean in_ca = true;
      });
      assertNotSame(getContents(root, "simple.txt"), sw.toString());
    }
    {
      Mustache m = c.compile("simple.html");
      StringWriter sw = new StringWriter();
      m.execute(sw, new Object() {
        public String name = "Chris";
        public int value = 10000;

        // It won't find this since it isn't public
        int taxed_value() {
          return (int) (this.value - (this.value * 0.4));
        }

        public boolean in_ca = true;
      });
      assertNotSame(getContents(root, "simple.txt"), sw.toString());
    }
    {
      assertThrows(MustacheException.class, () -> {
        Mustache m = c.compile(new StringReader("{{toString}}"), "test");
        m.execute(new StringWriter(), new Object() {
          public String toString() {
            return "";
          }
        });
      });
    }
    {
      assertThrows("Can't access that partial", MustacheException.class, () -> {
        Mustache m = c.compile(new StringReader("{{>/etc/passwd}}"), "test");
        m.execute(new StringWriter(), new Object() {
        });
      });
    }
    {
      assertThrows("Can't use pragmas", MustacheException.class, () -> {
        Mustache m = c.compile(new StringReader("{{%pragma}}"), "test");
        m.execute(new StringWriter(), new Object() {
        });
      });
    }
    {
      assertThrows("Can't use raw text", MustacheException.class, () -> {
        Mustache m = c.compile(new StringReader("{{{rawtext}}}"), "test");
        m.execute(new StringWriter(), new Object() {
          public String rawtext() {
            return "";
          }
        });
      });
    }
    {
      assertThrows("Can't change delimiters", MustacheException.class, () -> {
        Mustache m = c.compile(new StringReader("{{=[[]]=}}"), "test");
        m.execute(new StringWriter(), new Object() {});
      });
    }
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

  @Test
  public void testSimpleI18N() throws MustacheException, IOException {
    {
      MustacheFactory c = new DefaultMustacheFactory(new LocalizedMustacheResolver(root, Locale.KOREAN));
      Mustache m = c.compile("simple.html");
      StringWriter sw = new StringWriter();
      m.execute(sw, new Object() {
        final String name = "Chris";
        final int value = 10000;

        int taxed_value() {
          return (int) (this.value - (this.value * 0.4));
        }

        final boolean in_ca = true;
      });
      assertEquals(getContents(root, "simple_ko.txt"), sw.toString());
    }
    {
      MustacheFactory c = new DefaultMustacheFactory(new LocalizedMustacheResolver(root, Locale.JAPANESE));
      Mustache m = c.compile("simple.html");
      StringWriter sw = new StringWriter();
      m.execute(sw, new Object() {
        final String name = "Chris";
        final int value = 10000;

        int taxed_value() {
          return (int) (this.value - (this.value * 0.4));
        }

        final boolean in_ca = true;
      });
      assertEquals(getContents(root, "simple.txt"), sw.toString());
    }
  }

  @Test
  public void testRootCheck() throws MustacheException {
    MustacheFactory c = createMustacheFactory();
    assertThrows(MustacheException.class, () -> c.compile("../../../pom.xml"));
  }

  @Test
  public void testIssue280() throws IOException {
    MustacheFactory c = createMustacheFactory();
    StringWriter sw = new StringWriter();
    Mustache m = c.compile(new StringReader("{{{template}}}"), "test");
    Object o = new Object() {
      String template() throws IOException {
        MustacheFactory c = createMustacheFactory();
        StringWriter sw = new StringWriter();
        Mustache m = c.compile("template.html");
        m.execute(sw, new Object()).flush();
        return sw.toString();
      }
    };
    m.execute(sw, o).flush();
    assertTrue(sw.toString().startsWith("<html>"));
  }

  @Test
  public void testSimpleFiltered() throws MustacheException, IOException {
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
      final String name = "Chris";
      final int value = 10000;

      int taxed_value() {
        return (int) (this.value - (this.value * 0.4));
      }

      final boolean in_ca = true;
    });
    assertEquals(getContents(root, "simplefiltered.txt"), sw.toString());
  }

  @Test
  public void testTypedSimple() throws MustacheException, IOException {
    final Object scope = new Object() {
      final String name = "Chris";
      final int value = 10000;

      class MyObject {
        int taxed_value() {
          return (int) (value - (value * 0.4));
        }

        final String fred = "";
      }

      final MyObject in_ca = new MyObject();

      final boolean test = false;
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

  @Test
  public void testRecurision() throws IOException {
    StringWriter sw = execute("recursion.html", new Object() {
      final Object value = new Object() {
        final boolean value = false;
      };
    });
    assertEquals(getContents(root, "recursion.txt"), sw.toString());
  }

  @Test
  public void testRecursionWithInheritance() throws IOException {
    StringWriter sw = execute("recursion_with_inheritance.html", new Object() {
      final Object value = new Object() {
        final boolean value = false;
      };
    });
    assertEquals(getContents(root, "recursion.txt"), sw.toString());
  }

  @Test
  public void testPartialRecursionWithInheritance() throws IOException {
    StringWriter sw = execute("recursive_partial_inheritance.html", new Object() {
      final Object test = new Object() {
        final boolean test = false;
      };
    });
    assertEquals(getContents(root, "recursive_partial_inheritance.txt"), sw.toString());
  }

  @Test
  public void testChainedInheritance() throws IOException {
    StringWriter sw = execute("page.html", new Object() {
      final Object test = new Object() {
        final boolean test = false;
      };
    });
    assertEquals(getContents(root, "page.txt"), sw.toString());
  }

  @Test
  public void testDefaultValue() {
    DefaultMustacheFactory mf = new DefaultMustacheFactory(root);
    mf.setObjectHandler(new ReflectionObjectHandler() {
      @Override
      public Wrapper find(String name, List<Object> scopes) {
        int i;
        if ((i = name.indexOf("|")) != -1) {
          String newName = name.substring(0, i);
          String defaultValue = name.substring(i + 1);
          Wrapper wrapper = super.find(newName, scopes);
          if (wrapper instanceof MissingWrapper) {
            return scopes1 -> {
              // Test the guards returned in the missing wrapper
              wrapper.call(scopes1);
              return defaultValue;
            };
          }
          return wrapper;
        }
        return super.find(name, scopes);
      }
    });
    Mustache m = mf.compile(new StringReader("{{test}} {{test2|bar}} {{test3|baz}}"), "testDefaultValue");
    StringWriter sw = new StringWriter();
    m.execute(sw, new Object() {
      final String test = "foo";
      final String test2 = "BAR";
    });
    assertEquals("foo BAR baz", sw.toString());
  }

  @Test
  public void testSimplePragma() throws MustacheException, IOException {
    MustacheFactory c = createMustacheFactory();
    Mustache m = c.compile("simplepragma.html");
    StringWriter sw = new StringWriter();
    m.execute(sw, new Object() {
      final String name = "Chris";
      final int value = 10000;

      int taxed_value() {
        return (int) (this.value - (this.value * 0.4));
      }

      final boolean in_ca = true;
    });
    assertEquals(getContents(root, "simple.txt"), sw.toString());
  }

  private class OkGenerator {
    public boolean isItOk() {
      return true;
    }
  }

  @Test
  public void testNestedAccessWithSimpleObjectHandler() {
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

  @Test
  public void testClosingReader() {
    final AtomicBoolean closed = new AtomicBoolean();
    StringReader reader = new StringReader("{{test") {
      @Override
      public void close() {
        closed.set(true);
      }
    };
    MustacheFactory mf = new DefaultMustacheFactory();
    assertThrows(MustacheException.class, () -> mf.compile(reader, "test"));
    // The reader should be closed now
    assertTrue(closed.get());
  }

  @Test
  public void testMultipleWrappers() throws MustacheException, IOException {
    MustacheFactory c = createMustacheFactory();
    Mustache m = c.compile("simple.html");
    StringWriter sw = new StringWriter();
    m.execute(sw, new Object() {
      final String name = "Chris";
      final int value = 10000;

      final Object o = new Object() {
        int taxed_value() {
          return (int) (value - (value * 0.4));
        }

        final String fred = "test";
      };

      final Object in_ca = Arrays.asList(
              o, new Object() {
                final int taxed_value = (int) (value - (value * 0.2));
              },
              o
      );
    });
    assertEquals(getContents(root, "simplerewrap.txt"), sw.toString());
  }

  @Test
  public void testNestedLatchesIterable() throws IOException {
    DefaultMustacheFactory c = createMustacheFactory();
    c.setExecutorService(Executors.newCachedThreadPool());
    Mustache m = c.compile("latchedtestiterable.html");
    StringWriter sw = new StringWriter();
    final StringBuffer sb = new StringBuffer();
    final CountDownLatch cdl1 = new CountDownLatch(1);
    final CountDownLatch cdl2 = new CountDownLatch(1);

    m.execute(sw, new Object() {
      final Iterable list = Arrays.asList(
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

  @Test
  public void testConcurrency() throws IOException {
    DefaultMustacheFactory c = createMustacheFactory();
    c.setExecutorService(Executors.newCachedThreadPool());
    Mustache m = c.compile(new StringReader("{{a}} {{#caps}}{{b}}{{/caps}} {{c}}"), "concurrency");
    StringWriter sw = new StringWriter();
    long start = System.currentTimeMillis();
    Writer execute = m.execute(sw, new Object() {
      final Callable<Object> a = () -> {
        Thread.sleep(300);
        return "How";
      };
      final Callable<Object> b = () -> {
        Thread.sleep(200);
        return "are";
      };
      final Callable<Object> c = () -> {
        Thread.sleep(100);
        return "you?";
      };
      final Callable<Function> caps = () -> (Function) o -> o.toString().toUpperCase();
    });
    execute.close();
    assertTrue("Time < 600ms", System.currentTimeMillis() - start < 600);
    assertEquals("How ARE you?", sw.toString());
  }

  @Test
  public void testNestedLatches() throws IOException {
    DefaultMustacheFactory c = createMustacheFactory();
    c.setExecutorService(Executors.newCachedThreadPool());
    Mustache m = c.compile("latchedtest.html");
    StringWriter sw = new StringWriter();
    Writer execute = m.execute(sw, new Object() {
      final Callable<Object> nest = () -> {
        Thread.sleep(300);
        return "How";
      };
      final Callable<Object> nested = () -> {
        Thread.sleep(200);
        return "are";
      };
      final Callable<Object> nestest = () -> {
        Thread.sleep(100);
        return "you?";
      };
    });
    execute.close();

    assertEquals("<outer>\n<inner>How</inner>\n<inner>are</inner>\n<inner>you?</inner>\n</outer>\n", sw.toString());
  }

  @Test
  public void testBrokenSimple() throws MustacheException {
    assertThrows(MustacheException.class, () -> {
      MustacheFactory c = createMustacheFactory();
      Mustache m = c.compile("brokensimple.html");
      StringWriter sw = new StringWriter();
      m.execute(sw, new Object() {
        final String name = "Chris";
        final int value = 10000;

        int taxed_value() {
          return (int) (this.value - (this.value * 0.4));
        }

        final boolean in_ca = true;
      });
    });
  }

  @Test
  public void testIsNotEmpty() throws IOException {
    Object object = new Object() {
      final List<String> people = singletonList("Test");
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

  @Test
  public void testImmutableList() throws IOException {
    Object object = new Object() {
      final List<String> people = singletonList("Test");
    };
    StringWriter sw = execute("isempty.html", singletonList(object));
    assertEquals(getContents(root, "isempty.txt"), sw.toString());
  }

  @Test
  public void testOptional() {
    MustacheFactory c = createMustacheFactory();
    StringReader template = new StringReader("{{person}}{{#person}} is present{{/person}}{{^person}}Is not present{{/person}}");
    Mustache m = c.compile(template, "test");
    StringWriter sw = new StringWriter();
    m.execute(sw, new Object() {
      final Optional<String> person = Optional.of("Test");
    });
    assertEquals("Test is present", sw.toString());
    sw = new StringWriter();
    m.execute(sw, new Object() {
      final Optional<String> person = Optional.empty();
    });
    assertEquals("Is not present", sw.toString());
  }

  @Test
  public void testComment() {
    MustacheFactory c = createMustacheFactory();
    Mustache m = c.compile(new StringReader("{{#process}}{{!comment}}{{/process}}"), "");
    StringWriter sw = new StringWriter();
    m.execute(sw, new Object() {
      final TemplateFunction process = s -> s.replace("{", "[");
    });
    assertEquals("[[!comment}}", sw.toString());
  }

  @Test
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
      final int zero = 0;
      final int one = 1;
    }).close();
    assertEquals("onezero", sw.toString());
  }

  @Test
  public void testSecurity() throws MustacheException, IOException {
    MustacheFactory c = createMustacheFactory();
    Mustache m = c.compile("security.html");
    StringWriter sw = new StringWriter();
    m.execute(sw, new Object() {
      final String name = "Chris";
      final int value = 10000;

      int taxed_value() {
        return (int) (this.value - (this.value * 0.4));
      }

      final boolean in_ca = true;

      // Should not be accessible
      private final String test = "Test";
    });
    assertEquals(getContents(root, "security.txt"), sw.toString());
  }

  @Test
  public void testIdentitySimple() throws MustacheException, IOException {
    MustacheFactory c = createMustacheFactory();
    Mustache m = c.compile("simple.html");
    StringWriter sw = new StringWriter();
    m.identity(sw);
    assertEquals(getContents(root, "simple.html").replaceAll("\\s+", ""), sw.toString().replaceAll(
            "\\s+", ""));
  }

  @Test
  public void testProperties() throws MustacheException, IOException {
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

  @Test
  public void testSimpleWithMap() throws MustacheException, IOException {
    StringWriter sw = execute("simple.html", new HashMap<String, Object>() {{
      put("name", "Chris");
      put("value", 10000);
      put("taxed_value", 6000);
      put("in_ca", true);
    }});
    assertEquals(getContents(root, "simple.txt"), sw.toString());
  }

  @Test
  public void testReflectiveAccessThroughInterface() {
    MustacheFactory c = createMustacheFactory();
    Mustache m = c.compile(new StringReader("{{#entries}}{{key}}{{/entries}}"), "");
    StringWriter sw = new StringWriter();
    m.execute(sw, new Object() {
      final Set entries = new HashMap() {{
        put("key", "value");
      }}.entrySet();
    });
    assertEquals("key", sw.toString());
  }

  @Test
  public void testPartialWithTF() throws MustacheException {
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

  @Test
  public void testFunctions() throws IOException {
    MustacheFactory c = createMustacheFactory();
    Mustache m = c.compile(new StringReader("{{#f}}{{foo}}{{/f}}"), "test");
    {
      StringWriter sw = new StringWriter();
      m.execute(sw, new Object() {
        final Function f = new Function<String, String>() {
          @Override
          public String apply(String s) {
            return s.toUpperCase();
          }
        };
        final String foo = "bar";
      }).flush();
      assertEquals("BAR", sw.toString());
    }
    {
      StringWriter sw = new StringWriter();
      m.execute(sw, new Object() {
        final Function f = new TemplateFunction() {
          @Override
          public String apply(String s) {
            return s.toUpperCase();
          }
        };
        final String foo = "bar";
        final String FOO = "baz";
      }).flush();
      assertEquals("baz", sw.toString());
    }
  }

  @Test
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

  @Test
  public void testComplexParallel() throws MustacheException, IOException {
    MustacheFactory c = initParallel();
    Mustache m = c.compile("complex.html");
    StringWriter sw = new StringWriter();
    m.execute(sw, new ParallelComplexObject()).close();
    assertEquals(getContents(root, "complex.txt"), sw.toString());
  }

  @Test
  public void testSerialCallable() throws MustacheException, IOException {
    StringWriter sw = execute("complex.html", new ParallelComplexObject());
    assertEquals(getContents(root, "complex.txt"), sw.toString());
  }

  @Test
  public void testDynamicPartial() throws MustacheException, IOException {
    // Implement >+ syntax that dynamically includes template files at runtime
    MustacheFactory c = new DefaultMustacheFactory(root) {
      @Override
      public MustacheVisitor createMustacheVisitor() {
        return new DefaultMustacheVisitor(this) {
          @Override
          public void partial(TemplateContext tc, String variable, String indent) {
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

                final ConcurrentMap<String, Mustache> dynamicaPartialCache = new ConcurrentHashMap<>();

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
              super.partial(tc, variable, indent);
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
  @Test
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
  @Test
  public void testReadme() throws MustacheException, IOException {
    MustacheFactory c = createMustacheFactory();
    Mustache m = c.compile("items.html");
    StringWriter sw = new StringWriter();
    long start = System.currentTimeMillis();
    m.execute(sw, new Context());
    long diff = System.currentTimeMillis() - start;
    assertEquals(getContents(root, "items.txt"), sw.toString());
  }

  @Test
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

  @Test
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

      Callable<String> desc() {
        return () -> {
          Thread.sleep(1000);
          return description;
        };
      }
    }
  }

  @Test
  public void testDeferred() throws IOException {
    DefaultMustacheFactory mf = new DeferringMustacheFactory(root);
    mf.setExecutorService(Executors.newCachedThreadPool());
    Object context = new Object() {
      final String title = "Deferred";
      final Object deferred = new DeferringMustacheFactory.DeferredCallable();
      final Object deferredpartial = DeferringMustacheFactory.DEFERRED;
    };
    Mustache m = mf.compile("deferred.html");
    StringWriter sw = new StringWriter();
    m.execute(sw, context).close();
    assertEquals(getContents(root, "deferred.txt"), sw.toString());
  }

  @Test
  public void testMultipleCallsWithDifferentScopes() {
    String template = "Value: {{value}}";
    Mustache mustache = new DefaultMustacheFactory().compile(new StringReader(
            template), "test");

    // scope object doesn't have a 'value' property, lookup will fail
    mustache.execute(new StringWriter(), new Object());

    // scope object has a 'value' property, lookup shouldn't fail
    StringWriter sw = new StringWriter();
    mustache.execute(sw, new Object() {
      final String value = "something";
    });

    assertEquals("Value: something", sw.toString());
  }

  @Test
  public void testMultipleCallsWithDifferentMapScopes() {
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

  @Test
  public void testRelativePathsSameDir() throws IOException {
    MustacheFactory mf = createMustacheFactory();
    Mustache compile = mf.compile("relative/paths.html");
    StringWriter sw = new StringWriter();
    compile.execute(sw, "").close();
    assertEquals(getContents(root, "relative/paths.txt"), sw.toString());
  }

  @Test
  public void testRelativePathsRootDir() throws IOException {
    MustacheFactory mf = createMustacheFactory();
    Mustache compile = mf.compile("relative/rootpath.html");
    StringWriter sw = new StringWriter();
    compile.execute(sw, "").close();
    assertEquals(getContents(root, "relative/paths.txt"), sw.toString());
  }

  @Test
  public void testPathsWithExtension() throws IOException {
    MustacheFactory mf = createMustacheFactory();
    Mustache compile = mf.compile("relative/extension.html");
    StringWriter sw = new StringWriter();
    compile.execute(sw, "").close();
    assertEquals(getContents(root, "relative/paths.txt"), sw.toString());
  }

  @Test
  public void testRelativePathsTemplateFunction() throws IOException {
    MustacheFactory mf = createMustacheFactory();
    Mustache compile = mf.compile("relative/functionpaths.html");
    StringWriter sw = new StringWriter();
    compile.execute(sw, new Object() {
      final Function i = new TemplateFunction() {
        @Override
        public String apply(String s) {
          return s;
        }
      };
    }).close();
    assertEquals(getContents(root, "relative/paths.txt"), sw.toString());
  }

  @Test
  public void testRelativePathFail() {
    MustacheFactory mf = createMustacheFactory();
    assertThrows(MustacheException.class, () -> mf.compile("relative/pathfail.html"));
  }

  @Test
  public void testVariableInhertiance() throws IOException {
    DefaultMustacheFactory mf = createMustacheFactory();
    Mustache m = mf.compile("issue_201/chat.html");
    StringWriter sw = new StringWriter();
    m.execute(sw, new Object()).close();
    assertEquals("<script src=\"test\"></script>", sw.toString());
  }

  @Test
  public void testIterator() throws IOException {
    {
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
    {
      MustacheFactory mf = new DefaultMustacheFactory(root) {
        @Override
        public MustacheVisitor createMustacheVisitor() {
          return new DefaultMustacheVisitor(this) {
            @Override
            public void iterable(TemplateContext templateContext, String variable, Mustache mustache) {
              list.add(new IterableCode(templateContext, df, mustache, variable) {
                @Override
                protected boolean addScope(List<Object> scopes, Object scope) {
                  scopes.add(scope);
                  return true;
                }
              });
            }
          };
        }
      };
      Mustache m = mf.compile(new StringReader("{{#values}}{{.}}{{/values}}{{^values}}Test2{{/values}}"), "testIterator");
      StringWriter sw = new StringWriter();
      m.execute(sw, new Object() {
        Iterator values() {
          return Arrays.asList(1, null, 3).iterator();
        }
      }).close();
      assertEquals("13", sw.toString());
    }
  }

  @Test
  public void testObjectArray() throws IOException {
    MustacheFactory mf = createMustacheFactory();
    Mustache m = mf.compile(new StringReader("{{#values}}{{.}}{{/values}}{{^values}}Test2{{/values}}"), "testObjectArray");
    StringWriter sw = new StringWriter();
    m.execute(sw, new Object() {
      final Integer[] values = new Integer[]{1, 2, 3};
    }).close();
    assertEquals("123", sw.toString());
  }

  @Test
  public void testBaseArray() throws IOException {
    MustacheFactory mf = createMustacheFactory();
    Mustache m = mf.compile(new StringReader("{{#values}}{{.}}{{/values}}{{^values}}Test2{{/values}}"), "testBaseArray");
    StringWriter sw = new StringWriter();
    m.execute(sw, new Object() {
      final int[] values = new int[]{1, 2, 3};
    }).close();
    assertEquals("123", sw.toString());
  }

  @Test
  public void testEmptyString() throws IOException {
    MustacheFactory mf = createMustacheFactory();
    Mustache m = mf.compile(new StringReader("{{#values}}Test1{{/values}}{{^values}}Test2{{/values}}"), "testEmptyString");
    StringWriter sw = new StringWriter();
    m.execute(sw, new Object() {
      final String values = "";
    }).close();
    assertEquals("Test2", sw.toString());
  }

  @Test
  public void testPrivate() throws IOException {
    MustacheFactory mf = createMustacheFactory();
    Mustache m = mf.compile(new StringReader("{{#values}}Test1{{/values}}{{^values}}Test2{{/values}}"), "testPrivate");
    StringWriter sw = new StringWriter();
    m.execute(sw, new Object() {
      private final String values = "value";

      private String values() {
        return "value";
      }
    }).close();
    // Values ignored as if it didn't exist at all
    assertEquals("Test2", sw.toString());
  }

  @Test
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

  @Test
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

  @Test
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

  @Test
  public void testNotIterableCallable() throws IOException {
    MustacheFactory mf = createMustacheFactory();
    Mustache m = mf.compile(new StringReader("{{^value}}test{{/value}}"), "testNotIterableCallable");
    StringWriter sw = new StringWriter();
    m.execute(sw, new Object() {
      final Callable value = () -> null;
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

  @Test
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
      assertThrows(MustacheException.class, accessTrackingMap::check);
    }
  }

  private AccessTrackingMap createBaseMap() {
    AccessTrackingMap accessTrackingMap = new AccessTrackingMap();
    accessTrackingMap.put("first", "Sam");
    accessTrackingMap.put("last", "Pullara");
    return accessTrackingMap;
  }

  @Test
  public void testMismatch() {
    MustacheFactory mf = createMustacheFactory();
    assertThrows(MustacheException.class, () -> mf.compile(new StringReader("{{#value}}"), "testMismatch"));
    assertThrows(MustacheException.class, () -> mf.compile(new StringReader("{{#value}}{{/values}}"), "testMismatch"));
  }

  @Test
  public void testInvalidDelimiters() {
    assertThrows(MustacheException.class, () -> {
      MustacheFactory mf = createMustacheFactory();
      mf.compile(new StringReader("{{=toolong}}"), "testInvalidDelimiters");
    });
  }

  @Test
  public void testEmptyDot() {
    MustacheFactory mf = createMustacheFactory();
    StringWriter sw = new StringWriter();
    Mustache mustache = mf.compile(new StringReader("{{No.}}"), "template");
    Map<String, String> scope = new HashMap<>();
    scope.put("No", "1");
    mustache.execute(sw, scope);
    System.out.println(sw);
  }

  @Test
  public void testCase() {
    String template = "Hello {{user.name}}";

    DefaultMustacheFactory factory = new DefaultMustacheFactory();
    Mustache m = factory.compile(new StringReader(template), "test");

    Map<String, Object> scopes = new HashMap<>();
    Map<String, String> value = new HashMap<>();
    value.put("name", "Test");
    scopes.put("user", value);

    //Expected this to output 'Hello'
    String result =  m.execute(new StringWriter(), (Map<String, Object>) null).toString();
    System.out.println(result); //prints 'Hello'

    //Expected this to output 'Hello Test' instead prints 'Hello'
    result = m.execute(new StringWriter(), scopes).toString();
    System.out.println(result); //prints 'Hello'
  }

  @Test
  public void testTemplateFunction() throws IOException {
    MustacheFactory mf = createMustacheFactory();
    Mustache m = mf.compile(new StringReader("{{#i}}{{{test}}}{{f}}{{/i}}" +
            "{{#comment}}comment{{/comment}}"), "testTemplateFunction");
    StringWriter sw = new StringWriter();
    m.execute(sw, new Object() {
      final Function i = new TemplateFunction() {
        @Override
        public String apply(String s) {
          return s.replace("test", "test2");
        }
      };
      final String test2 = "test";
      final Function f = o -> null;
      final CommentFunction comment = new CommentFunction();
    }).close();
    // Values ignored as if it didn't exist at all
    assertEquals("test", sw.toString());
  }

  private static class SuperClass {
    String values = "value";
  }

  @Test
  public void testSuperField() throws IOException {
    MustacheFactory mf = createMustacheFactory();
    Mustache m = mf.compile(new StringReader("{{#values}}Test1{{/values}}{{^values}}Test2{{/values}}"), "testIterator");
    StringWriter sw = new StringWriter();
    m.execute(sw, new SuperClass() {
    }).close();
    // Values ignored as if it didn't exist at all
    assertEquals("Test1", sw.toString());
  }

  @Test
  public void testRelativePathsDotDotDir() throws IOException {
    MustacheFactory mf = createMustacheFactory();
    Mustache compile = mf.compile("relative/dotdot.html");
    StringWriter sw = new StringWriter();
    compile.execute(sw, "").close();
    assertEquals(getContents(root, "uninterestingpartial.html"), sw.toString());
  }

  @Test
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

  @Test
  public void testOverrideExtension() throws IOException {
    MustacheFactory mf = new DefaultMustacheFactory(root) {
      @Override
      public MustacheVisitor createMustacheVisitor() {
        return new DefaultMustacheVisitor(this) {
          @Override
          public void partial(TemplateContext tc, String variable, String indent) {
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

  @Test
  public void testEmptyMustache() {
    MustacheException e = assertThrows(MustacheException.class, () -> new DefaultMustacheFactory().compile(new StringReader("{{}}"), "test"));
    assertTrue(e.getMessage().startsWith("Empty mustache"));
  }

  @Test
  public void testMustacheNotFoundException() {
    String nonExistingMustache = "404";
    MustacheNotFoundException e = assertThrows(MustacheNotFoundException.class, () -> new DefaultMustacheFactory().compile(nonExistingMustache));
    assertEquals(nonExistingMustache, e.getName());
  }

  @Test
  public void testImplicitIteratorNoScope() throws IOException {
    Mustache test = new DefaultMustacheFactory().compile(new StringReader("{{.}}"), "test");
    StringWriter sw = new StringWriter();
    test.execute(sw, "").close();
    assertEquals("", sw.toString());
    StringWriter sw2 = new StringWriter();
    test.execute(sw2, new Object[0]).close();
    assertEquals("", sw2.toString());
  }

  @Test
  public void testImplicitIteratorWithScope() throws IOException {
    Mustache test = new DefaultMustacheFactory().compile(new StringReader("{{#test}}_{{.}}_{{/test}}"), "test");
    StringWriter sw = new StringWriter();
    test.execute(sw, new Object() {
      final List<String> test = Arrays.asList("a", "b", "c");
    }).close();
    assertEquals("_a__b__c_", sw.toString());
  }

  @Test
  public void testCR() {
    Mustache m = new DefaultMustacheFactory().compile(new StringReader("{{test}}\r\n{{test}}\r\n"), "test");
    StringWriter sw = new StringWriter();
    m.execute(sw, new Object() {
      final String test = "fred";
    });
    assertEquals("fred\r\nfred\r\n", sw.toString());
  }

  @Test
  public void testOutputDelimiters() {
    String template = "{{=## ##=}}{{##={{ }}=####";
    Mustache mustache = new DefaultMustacheFactory().compile(new StringReader(template), "test");
    StringWriter sw = new StringWriter();
    mustache.execute(sw, new Object[0]);
    assertEquals("{{##", sw.toString());
  }

  @Test
  public void testImproperlyClosedVariable() {
    MustacheException actual = assertThrows(MustacheException.class, () -> new DefaultMustacheFactory().compile(new StringReader("{{{#containers}} {{/containers}}"), "example"));
    assertEquals("Improperly closed variable: #containers in example:1@16 @[example:1]", actual.getMessage());
  }

  @Test
  public void testLambdaExceptions() {
    assertThrows(MustacheException.class, () -> {
      String template = "hello {{#timesTwo}}a{{/timesTwo}}";
      Mustache mustache = new DefaultMustacheFactory().compile(new StringReader(template), "test");
      StringWriter sw = new StringWriter();
      mustache.execute(sw, new Object() {
        final Function<String, String> timesTwo = (s) -> {
          throw new RuntimeException();
        };
      });
    });
  }

  @Test
  public void testDirectoryInsteadOfFile() {
    assertThrows(MustacheException.class, () -> {
      // there is a folder called "templates" in the resources dir (src/main/resources/templates)
      MustacheFactory mustacheFactory = new DefaultMustacheFactory();
      Mustache template = mustacheFactory.compile("templates");
    });
  }

  @Test
  public void testLimitedDepthRecursion() {
    // Should not throw a StackOverflowError
    MustacheException e = assertThrows(MustacheException.class, () -> execute("infiniteparent.html", new Context()));
    assertEquals("Maximum partial recursion limit reached: 100 @[infiniteparent.html:1]", e.getMessage());
  }

  @Test
  public void testIssue191() throws IOException {
    MustacheFactory mustacheFactory = createMustacheFactory();
    Mustache mustache = mustacheFactory.compile("templates/someTemplate.mustache");
    StringWriter stringWriter = new StringWriter();
    mustache.execute(stringWriter, singletonMap("title", "Some title!"));
    assertEquals(getContents(root, "templates/someTemplate.txt"), stringWriter.toString());
  }

  @Test
  public void testMalformedTag() {
    MustacheException e = assertThrows(MustacheException.class, () -> {
      String template = "\n{{$value}}\n{/value}}";
      Mustache mustache = new DefaultMustacheFactory().compile(new StringReader(template), "test");
      StringWriter sw = new StringWriter();
      mustache.execute(sw, new Object[0]);
    });
    assertEquals("Failed to close 'value' tag @[test:2]", e.getMessage());
  }

  @Test
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

      final String anotherVar = "banana";
      final String yetAnotherVar = "apple";
    });
    assertEquals("blablabla banana, blablabla apple", sw.toString());
  }

  @Test
  public void testTemplateFunctionWithImplicitParams() {
    String template = "{{#parse}}\n" +
            "{{replaceMe}}\n" +
            "{{/parse}}";
    DefaultMustacheFactory mf = new DefaultMustacheFactory() {
      public MustacheVisitor createMustacheVisitor() {
        return new DefaultMustacheVisitor(this) {
          public void iterable(final TemplateContext templateContext, String variable, Mustache mustache) {
            list.add(new IterableCode(templateContext, df, mustache, variable) {
              final Binding binding = oh.createBinding("params", templateContext, this);

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

      final Map<String, Object> params = new HashMap<>();

      {
        params.put("anotherVar", "banana");
        params.put("yetAnotherVar", "apple");
      }
    });
    assertEquals("blablabla banana, blablabla apple", sw.toString());
  }

  @Test
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
    {
      sr = new StringReader("{{value}}");
      Mustache m = mf.compile(sr, "value");
      assertThrows(MustacheException.class, () -> m.execute(new StringWriter(), new Object() {}));
    }
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    PrintStream ps = new PrintStream(out);
    System.setErr(ps);
    {
      sr = new StringReader("{{value}}");
      Mustache m = mf.compile(sr, "value");
      m.execute(new StringWriter(), new Object() {
        final String value = null;
      }).close();
      ps.flush();
      assertTrue(new String(out.toByteArray()).contains("Variable is null"));
    }
    out.reset();
    {
      sr = new StringReader("{{value}}");
      Mustache m = mf.compile(sr, "value");
      m.execute(new StringWriter(), new Object() {
        final String value = "";
      }).close();
      ps.flush();
      assertTrue(new String(out.toByteArray()).contains("Variable is empty string"));
    }
    out.reset();
    {
      sr = new StringReader("{{value}}");
      Mustache m = mf.compile(sr, "value");
      StringWriter sw = new StringWriter();
      m.execute(sw, new Object() {
        final String value = "value";
      }).close();
      ps.flush();
      assertEquals("", new String(out.toByteArray()));
      assertEquals("value", sw.toString());
    }
  }

  @Test
  public void testPropertyWithDot() throws IOException {
    DefaultMustacheFactory mustacheFactory = new DefaultMustacheFactory();
    Reader reader = new StringReader("value=${some.value}");
    Mustache mustache = mustacheFactory.compile(reader, "maven", "${", "}");
    Map<String, String> properties = new HashMap<>();
    properties.put("some.value", "some.value");
    StringWriter writer = new StringWriter();
    mustache.execute(writer, new Object[]{properties}).close();
    assertEquals("value=some.value", writer.toString());
  }

  @Test
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
                    return writer;
                  } else {
                    return super.execute(writer, scopes);
                  }
                } catch (Exception e) {
                  throw new MustacheException("Failed to get value for " + name, e, tc);
                }
              }
            });
          }
        };
      }
    };
    Mustache test = dmf.compile(new StringReader("{{name}} - {{email}} 1"), "test");
    StringWriter sw = new StringWriter();
    Map<Object, Object> map = new HashMap<>();
    map.put("name", "Sam Pullara");
    test.execute(sw, map).close();
    assertEquals("Sam Pullara - {{email}} 1", sw.toString());
  }

  private DefaultMustacheFactory initParallel() {
    DefaultMustacheFactory cf = createMustacheFactory();
    cf.setExecutorService(Executors.newCachedThreadPool());
    return cf;
  }

  @Before
  public void setUp() throws Exception {
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

    Writer writer = new StringWriter();
    MustacheFactory mf = new DefaultMustacheFactory();
    Mustache mustache = mf.compile(new StringReader("{{#names}}<h2>First Name : {{name}}</h2> <h2>Last Name : {{last}}</h2>{{/names}}"), "example");
    mustache.execute(writer, map);
    writer.flush();
    assertEquals("<h2>First Name : firstName</h2> <h2>Last Name : lastName</h2><h2>First Name : firstName 1</h2> <h2>Last Name : lastName 1</h2>", writer.toString());
  }
}
