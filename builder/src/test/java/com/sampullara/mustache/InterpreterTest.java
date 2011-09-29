package com.sampullara.mustache;

import com.google.common.base.Function;
import com.google.common.util.concurrent.SettableFuture;
import com.sampullara.util.FutureWriter;
import com.sampullara.util.TemplateFunction;
import com.sampullara.util.http.JSONHttpRequest;
import junit.framework.TestCase;
import org.codehaus.jackson.JsonFactory;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.JsonParser;
import org.codehaus.jackson.map.MappingJsonFactory;

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
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;

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
    assertEquals(getContents(root, "simple.txt"), sw.toString());
  }

  public void testIdentitySimple() throws MustacheException, IOException, ExecutionException, InterruptedException {
    MustacheBuilder c = new MustacheBuilder(root);
    Mustache m = c.parseFile("simple.html");
    StringWriter sw = new StringWriter();
    FutureWriter writer = new FutureWriter(sw);
    m.execute(writer, IdentityScope.one);
    writer.flush();
    assertEquals(getContents(root, "simple.html").replaceAll("\\s+", ""), sw.toString().replaceAll(
            "\\s+", ""));
  }

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

  public void testProperties() throws MustacheException, IOException, ExecutionException, InterruptedException {
    MustacheBuilder c = new MustacheBuilder(root);
    Mustache m = c.parseFile("simple.html");
    StringWriter sw = new StringWriter();
    FutureWriter writer = new FutureWriter(sw);
    m.execute(writer, new Scope(new Object() {
      String getName() { return "Chris"; }
      int getValue() { return 10000; }

      int taxed_value() {
        return (int) (this.getValue() - (this.getValue() * 0.4));
      }

      boolean isIn_ca() { return true; }
    }));
    writer.flush();
    assertEquals(getContents(root, "simple.txt"), sw.toString());
  }

  public void testSimpleWithMap() throws MustacheException, IOException, ExecutionException, InterruptedException {
    MustacheBuilder c = new MustacheBuilder(root);
    Mustache m = c.parseFile("simple.html");
    StringWriter sw = new StringWriter();
    FutureWriter writer = new FutureWriter(sw);
    m.execute(writer, new HashMap<String, Object>() {{
      put("name", "Chris");
      put("value", 10000);
      put("taxed_value", 6000);
      put("in_ca", true);
    }});
    writer.flush();
    assertEquals(getContents(root, "simple.txt"), sw.toString());
  }

  public void testSimpleWithJson() throws MustacheException, IOException, ExecutionException, InterruptedException {
    MustacheBuilder c = new MustacheBuilder(root);
    Mustache m = c.parseFile("simple.html");
    StringWriter sw = new StringWriter();
    JsonFactory jf = new MappingJsonFactory();
    JsonNode jsonNode = jf.createJsonParser("{\"name\":\"Chris\", \"value\":10000, \"taxed_value\":6000,\"in_ca\":true}").readValueAsTree();
    m.execute(sw, jsonNode);
    assertEquals(getContents(root, "simple.txt"), sw.toString());
  }

  public void testSimpleAndWriter() throws MustacheException, IOException, ExecutionException, InterruptedException {
    MustacheBuilder c = new MustacheBuilder(root);
    Mustache m = c.parseFile("simple.html");
    StringWriter sw = new StringWriter();
    m.execute(sw, new Scope(new Object() {
      String name = "Chris";
      int value = 10000;

      int taxed_value() {
        return (int) (this.value - (this.value * 0.4));
      }

      boolean in_ca = true;
    }));
    assertEquals(getContents(root, "simple.txt"), sw.toString());
  }

  public void testSimpleWithMapAndWriter() throws MustacheException, IOException, ExecutionException, InterruptedException {
    MustacheBuilder c = new MustacheBuilder(root);
    Mustache m = c.parseFile("simple.html");
    StringWriter sw = new StringWriter();
    m.execute(sw, new HashMap<String, Object>() {{
      put("name", "Chris");
      put("value", 10000);
      put("taxed_value", 6000);
      put("in_ca", true);
    }});
    assertEquals(getContents(root, "simple.txt"), sw.toString());
  }

  public void testSimpleWithJsonAndWriter() throws MustacheException, IOException, ExecutionException, InterruptedException {
    MustacheBuilder c = new MustacheBuilder(root);
    Mustache m = c.parseFile("simple.html");
    StringWriter sw = new StringWriter();
    FutureWriter writer = new FutureWriter(sw);
    JsonFactory jf = new MappingJsonFactory();
    JsonNode jsonNode = jf.createJsonParser("{\"name\":\"Chris\", \"value\":10000, \"taxed_value\":6000,\"in_ca\":true}").readValueAsTree();
    m.execute(writer, jsonNode);
    writer.flush();
    assertEquals(getContents(root, "simple.txt"), sw.toString());
  }

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

  public void testEscaped() throws MustacheException, IOException {
    MustacheBuilder c = init();
    Mustache m = c.parseFile("escaped.html");
    StringWriter sw = new StringWriter();
    FutureWriter writer = new FutureWriter(sw);
    m.execute(writer, new Scope(new Object() {
      String title = "Bear > Shark";
      String entities = "&quot;";
    }));
    writer.flush();
    assertEquals(getContents(root, "escaped.txt"), sw.toString());
  }

  public void testUnescaped() throws MustacheException, IOException {
    MustacheBuilder c = new MustacheBuilder(root);
    Mustache m = c.parseFile("unescaped.html");
    StringWriter sw = new StringWriter();
    FutureWriter writer = new FutureWriter(sw);
    m.execute(writer, new Scope(new Object() {
      String title() {
        return "Bear > Shark";
      }
    }));
    writer.flush();
    assertEquals(getContents(root, "unescaped.txt"), sw.toString());
  }

  public void testInverted() throws MustacheException, IOException {
    MustacheBuilder c = init();
    Mustache m = c.parseFile("inverted_section.html");
    StringWriter sw = new StringWriter();
    FutureWriter writer = new FutureWriter(sw);
    m.execute(writer, new Scope(new Object() {
      String name() {
        return "Bear > Shark";
      }

      ArrayList repo = new ArrayList();
    }));
    writer.flush();
    assertEquals(getContents(root, "inverted_section.txt"), sw.toString());
  }

  public void testComments() throws MustacheException, IOException {
    MustacheBuilder c = init();
    Mustache m = c.parseFile("comments.html");
    StringWriter sw = new StringWriter();
    FutureWriter writer = new FutureWriter(sw);
    m.execute(writer, new Scope(new Object() {
      String title() {
        return "A Comedy of Errors";
      }
    }));
    writer.flush();
    assertEquals(getContents(root, "comments.txt"), sw.toString());
  }

  public void testPartial() throws MustacheException, IOException {
    MustacheBuilder c = init();
    Mustache m = c.parseFile("template_partial.html");
    StringWriter sw = new StringWriter();
    FutureWriter writer = new FutureWriter(sw);
    Scope scope = new Scope();
    scope.put("title", "Welcome");
    scope.put("template_partial_2", new Object() {
      String again = "Goodbye";
    });
    m.execute(writer, scope);
    writer.flush();
    assertEquals(getContents(root, "template_partial.txt"), sw.toString());
  }

  public static class PartialChanged extends Mustache {
    static AtomicBoolean executed = new AtomicBoolean(false);
    protected Mustache compilePartial(String name) throws MustacheException {
      executed.set(true);
      return super.compilePartial(name);
    }
  }

  public void testPartialOverride() throws MustacheException, IOException {
    MustacheBuilder c = init();
    c.setSuperclass(PartialChanged.class.getName());
    Mustache m = c.parseFile("template_partial.html");
    StringWriter sw = new StringWriter();
    FutureWriter writer = new FutureWriter(sw);
    Scope scope = new Scope();
    scope.put("title", "Welcome");
    scope.put("template_partial_2", new Object() {
      String again = "Goodbye";
    });
    m.execute(writer, scope);
    writer.flush();
    assertEquals(getContents(root, "template_partial.txt"), sw.toString());
    assertTrue(PartialChanged.executed.get());
  }

  public void testComplex() throws MustacheException, IOException {
    Scope scope = new Scope(new Object() {
      String header = "Colors";
      List item = Arrays.asList(
              new Object() {
                String name = "red";
                boolean current = true;
                String url = "#Red";
              },
              new Object() {
                String name = "green";
                boolean current = false;
                String url = "#Green";
              },
              new Object() {
                String name = "blue";
                boolean current = false;
                String url = "#Blue";
              }
      );

      boolean link(Scope s) {
        return !((Boolean) s.get("current"));
      }

      boolean list(Scope s) {
        return ((List) s.get("item")).size() != 0;
      }

      boolean empty(Scope s) {
        return ((List) s.get("item")).size() == 0;
      }
    });
    MustacheBuilder c = new MustacheBuilder(root);
    Mustache m = c.parseFile("complex.html");
    StringWriter sw = new StringWriter();
    FutureWriter writer = new FutureWriter(sw);
    m.execute(writer, scope);
    writer.flush();
    assertEquals(getContents(root, "complex.txt"), sw.toString());
  }

  public void testJson() throws IOException, MustacheException {
    String content = getContents(root, "template_partial.js");
    content = content.substring(content.indexOf("=") + 1);
    JsonParser jp = new MappingJsonFactory().createJsonParser(content);
    JsonNode jsonNode = jp.readValueAsTree();
    Scope scope = new Scope(jsonNode);
    MustacheBuilder c = init();
    Mustache m = c.parseFile("template_partial.html");
    StringWriter sw = new StringWriter();
    FutureWriter writer = new FutureWriter(sw);
    m.execute(writer, scope);
    writer.flush();
    assertEquals(getContents(root, "template_partial.txt"), sw.toString());

  }

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

      /*
       * verify null elements in a list are properly handled when using {{.}}
       */
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

  public void testReadme() throws MustacheException, IOException {
    MustacheBuilder c = init();
    Mustache m = c.parseFile("items.html");
    StringWriter sw = new StringWriter();
    FutureWriter writer = new FutureWriter(sw);
    long start = System.currentTimeMillis();
    m.execute(writer, new Scope(new Context()));
    writer.flush();
    long diff = System.currentTimeMillis() - start;
    assertEquals(getContents(root, "items.txt"), sw.toString());
  }

  public void testReadme2() throws MustacheException, IOException {
    MustacheBuilder c = new MustacheBuilder(root);
    Mustache m = c.parseFile("items2.html");
    StringWriter sw = new StringWriter();
    FutureWriter writer = new FutureWriter(sw);
    long start = System.currentTimeMillis();
    m.execute(writer, new Scope(new Context()));
    writer.flush();
    long diff = System.currentTimeMillis() - start;
    assertEquals(getContents(root, "items.txt"), sw.toString());
    assertTrue("Should be a little bit more than 1 second: " + diff, diff > 999 && diff < 2000);
  }

  public void testReadme3() throws MustacheException, IOException {
    MustacheBuilder c = new MustacheBuilder(root);
    Mustache m = c.parseFile("items3.html");
    StringWriter sw = new StringWriter();
    FutureWriter writer = new FutureWriter(sw);
    long start = System.currentTimeMillis();
    m.execute(writer, new Scope(new Context()));
    writer.flush();
    long diff = System.currentTimeMillis() - start;
    assertEquals(getContents(root, "items3.txt"), sw.toString());
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

      Future<String> desc() throws InterruptedException {
        final SettableFuture<String> result = SettableFuture.create();
        Timer timer = new Timer();
        timer.schedule(new TimerTask() {
          @Override
          public void run() {
            result.set(description);
          }
        }, 1000);
        return result;
      }
    }
  }

  public void testJSONHttpRequest() throws MustacheException, IOException {
    MustacheBuilder c = init();
    Mustache m = c.parseFile("simple2.html");
    StringWriter sw = new StringWriter();
    FutureWriter writer = new FutureWriter(sw);
    m.execute(writer, new Scope(new Object() {
      Future<JsonNode> data() throws IOException {
        JSONHttpRequest jhr = new JSONHttpRequest("http://www.javarants.com/simple.json");
        return jhr.execute();
      }
    }));
    writer.flush();
    assertEquals(getContents(root, "simple.txt"), sw.toString());
  }

  private MustacheBuilder init() {
    return new MustacheBuilder(root);
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
