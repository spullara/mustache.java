package com.github.mustachejava;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;

import com.google.common.base.Function;
import com.google.common.util.concurrent.SettableFuture;

import com.github.mustachejava.impl.DefaultCodeFactory;
import junit.framework.TestCase;

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
    MustacheCompiler c = init();
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

  public void testBrokenSimple() throws MustacheException, IOException, ExecutionException, InterruptedException {
    try {
      MustacheCompiler c = init();
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

  /*
  public void testSecurity() throws MustacheException, IOException, ExecutionException, InterruptedException {
    MustacheBuilder c = new MustacheBuilder(root);
    Mustache m = c.parseFile("security.html");
    StringWriter sw = new StringWriter();
    FutureWriter writer = new FutureWriter(sw);
    m.execute(writer, new Scope(new Object() {
      String name = "Chris";
      int value = 10000;

      int taxed_value() {
        return (int) (this.value - (this.value * 0.4));
      }

      boolean in_ca = true;

      // Should not be accessible
      private String test = "Test";
    }));
    writer.flush();
    assertEquals(getContents(root, "security.txt"), sw.toString());
  }
  */
  public void testIdentitySimple() throws MustacheException, IOException, ExecutionException, InterruptedException {
    MustacheCompiler c = init();
    Mustache m = c.compile("simple.html");
    StringWriter sw = new StringWriter();
    m.identity(sw);
    assertEquals(getContents(root, "simple.html").replaceAll("\\s+", ""), sw.toString().replaceAll(
            "\\s+", ""));
  }
  /*
  public void testClassLoaderSimple() throws MustacheException, IOException, ExecutionException, InterruptedException {
    MustacheBuilder c = new MustacheBuilder();
    classLoaderTest(c);
    MustacheBuilder c2 = new MustacheBuilder("path");
    classLoaderTest(c2);
  }

  private void classLoaderTest(MustacheBuilder c) throws MustacheException, IOException {
    Mustache m = c.parseFile("classloader.html");
    StringWriter sw = new StringWriter();
    FutureWriter writer = new FutureWriter(sw);
    m.execute(writer, new Scope(new Object() {
      String name = "Chris";
      int value = 10000;

      int taxed_value() {
        return (int) (this.value - (this.value * 0.4));
      }

      boolean in_ca = true;
    }));
    writer.flush();
    assertEquals(getContents(root, "simple.txt"), sw.toString());
  }

  public void testSimpleTwice() throws MustacheException, IOException, ExecutionException, InterruptedException {
    MustacheBuilder c = new MustacheBuilder(root);
    Mustache m = c.parseFile("simple.html");
    StringWriter sw = new StringWriter();
    FutureWriter writer = new FutureWriter(sw);
    m.execute(writer, new Scope(new Object() {
      String name = "Chris";
      int value = 10000;

      int taxed_value() {
        return (int) (this.value - (this.value * 0.4));
      }

      boolean in_ca = true;
    }));
    writer.flush();
    m.execute(writer, new Scope(new Object() {
      String name = "Chris";
      int value = 10000;

      int taxed_value() {
        return (int) (this.value - (this.value * 0.4));
      }

      boolean in_ca = true;
    }));
    writer.flush();
    String contents = getContents(root, "simple.txt");
    assertEquals(contents + contents, sw.toString());
  }
  */
  public void testProperties() throws MustacheException, IOException, ExecutionException, InterruptedException {
    MustacheCompiler c = init();
    Mustache m = c.compile("simple.html");
    StringWriter sw = new StringWriter();
    m.execute(sw, new Object() {
      String getName() { return "Chris"; }
      int getValue() { return 10000; }

      int taxed_value() {
        return (int) (this.getValue() - (this.getValue() * 0.4));
      }

      boolean isIn_ca() { return true; }
    });
    assertEquals(getContents(root, "simple.txt"), sw.toString());
  }

  public void testSimpleWithMap() throws MustacheException, IOException, ExecutionException, InterruptedException {
    MustacheCompiler c = init();
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
  /*

  public void testSimpleLamda() throws MustacheException, IOException {
    MustacheBuilder c = new MustacheBuilder(root);
    Mustache m = c.parseFile("lambda.html");
    StringWriter sw = new StringWriter();
    FutureWriter writer = new FutureWriter(sw);
    m.execute(writer, new Scope(new Object() {
      Function<String, String> translate = new Function<String, String>() {
        @Override
        public String apply(String input) {
          if (input.equals("Hello")) {
            return "Hola";
          } if (input.equals("Hola")) {
            return "Hello";
          }
          return null;
        }
      };
    }));
    writer.flush();
    assertEquals(getContents(root, "lambda.txt"), sw.toString());
  }

  public void testTemplateLamda() throws MustacheException, IOException {
    MustacheBuilder c = new MustacheBuilder(root);
    Mustache m = c.parseFile("templatelambda.html");
    StringWriter sw = new StringWriter();
    FutureWriter writer = new FutureWriter(sw);
    m.execute(writer, new Scope(new Object() {
      String name = "Sam";
      TemplateFunction translate = new TemplateFunction() {
        @Override
        public String apply(String input) {
          if (input.equals("Hello {{name}}")) {
            return "{{name}}, Hola!";
          } else if (input.equals("Hello {{>user}}!")) {
            return "Hola, {{>user}}!";
          }
          return null;
        }
      };
    }));
    writer.flush();
    assertEquals(getContents(root, "templatelambda.txt"), sw.toString());
  }

  public void testSimpleWithSave() throws MustacheException, IOException, ExecutionException, InterruptedException {
    MustacheBuilder c = init();
    Mustache m = c.parseFile("simple.html");
    StringWriter sw = new StringWriter();
    FutureWriter writer = new FutureWriter(sw);
    m.execute(writer, new Scope(new Object() {
      String name = "Chris";
      int value = 10000;

      int taxed_value() {
        return (int) (this.value - (this.value * 0.4));
      }

      boolean in_ca = true;
    }));
    writer.flush();
    assertEquals(getContents(root, "simple.txt"), sw.toString());
  }

  public void testMissing() throws MustacheException, IOException, ExecutionException, InterruptedException {
    MustacheBuilder c = init();
    Mustache m = c.parseFile("simple.html");
    StringWriter sw = new StringWriter();
    FutureWriter writer = new FutureWriter(sw);
    m.execute(writer, new Scope(new Object() {
    }));
    writer.flush();
    assertEquals(getContents(root, "simplemissing.txt"), sw.toString());
  }

  public void testSetWriter() throws MustacheException, IOException, ExecutionException, InterruptedException {
    MustacheBuilder c = init();
    Mustache m = c.parseFile("simple.html");
    FutureWriter writer = new FutureWriter();
    m.execute(writer, new Scope(new Object() {
      String name = "Chris";
      int value = 10000;

      int taxed_value() {
        return (int) (this.value - (this.value * 0.4));
      }

      boolean in_ca = true;
    }));
    StringWriter sw = new StringWriter();
    writer.setWriter(sw);
    writer.flush();
    assertEquals(getContents(root, "simple.txt"), sw.toString());
  }

  public void testSimple2() throws MustacheException, IOException, ExecutionException, InterruptedException {
    MustacheBuilder c = init();
    Mustache m = c.parseFile("simple.html");
    StringWriter sw = new StringWriter();
    FutureWriter writer = new FutureWriter(sw);
    m.execute(writer, new Scope(new Object() {
      String name = "Chris";
      int value = 10000;

      int taxed_value() {
        return (int) (this.value - (this.value * 0.4));
      }

      boolean in_ca = false;
    }));
    writer.flush();
    assertEquals(getContents(root, "simple2.txt"), sw.toString());
  }

  */
  public void testPartial() throws MustacheException, IOException {
    MustacheCompiler c = init();
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
  
  public void testComplex() throws MustacheException, IOException {
    MustacheCompiler c = init();
    Mustache m = c.compile("complex.html");
    StringWriter sw = new StringWriter();
    m.execute(sw, new ComplexObject());
    assertEquals(getContents(root, "complex.txt"), sw.toString());
  }

  private static class ComplexObject {
    String header = "Colors";
    List<Color> item = Arrays.asList(
            new Color("red", true, "#Red"),
            new Color("green", false, "#Green"),
            new Color("blue", false, "#Blue")
    );

    boolean list() {
      return item.size() != 0;
    }

    boolean empty() {
      return item.size() == 0;
    }

    private static class Color {
      boolean link() {
        return !current;
      }
      Color(String name, boolean current, String url) {
        this.name = name;
        this.current = current;
        this.url = url;
      }
      String name;
      boolean current;
      String url;
    }
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
    MustacheCompiler c = init();
    Mustache m = c.compile("items.html");
    StringWriter sw = new StringWriter();
    long start = System.currentTimeMillis();
    m.execute(sw, new Context());
    long diff = System.currentTimeMillis() - start;
    assertEquals(getContents(root, "items.txt"), sw.toString());
  }

  public void testReadmeSerial() throws MustacheException, IOException {
    MustacheCompiler c = init();
    Mustache m = c.compile("items2.html");
    StringWriter sw = new StringWriter();
    long start = System.currentTimeMillis();
    m.execute(sw, new Context());
    long diff = System.currentTimeMillis() - start;
    assertEquals(getContents(root, "items.txt"), sw.toString());
    assertTrue("Should be a little bit more than 4 seconds: " + diff, diff > 3999 && diff < 6000);
  }

  public void testReadmeParallel() throws MustacheException, IOException {
    MustacheCompiler c = initParallel();
    Mustache m = c.compile("items2.html");
    StringWriter sw = new StringWriter();
    long start = System.currentTimeMillis();
    m.execute(sw, new Context());
    long diff = System.currentTimeMillis() - start;
    assertEquals(getContents(root, "items.txt"), sw.toString());
    assertTrue("Should be a little bit more than 1 second: " + diff, diff > 999 && diff < 2000);
  }

  static class Context {
    List<Item> items() {
      return Arrays.asList(
              new Item("Item 1", "$19.99", Arrays.asList(new Feature("New!"), new Feature("Awesome!"))),
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

  private MustacheCompiler init() {
    DefaultCodeFactory cf = new DefaultCodeFactory(root);
    return new MustacheCompiler(cf);
  }

  private MustacheCompiler initParallel() {
    DefaultCodeFactory cf = new DefaultCodeFactory(root);
    cf.setExecutorService(Executors.newCachedThreadPool());
    return new MustacheCompiler(cf);
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
    File file = new File("src/test/resources");
    root = new File(file, "simple.html").exists() ? file : new File("../src/test/resources");
  }

}
