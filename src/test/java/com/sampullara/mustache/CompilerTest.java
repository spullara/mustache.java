package com.sampullara.mustache;

import com.sampullara.util.CallbackFuture;
import com.sampullara.util.FutureWriter;
import com.sampullara.util.http.JSONHttpRequest;
import junit.framework.TestCase;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.JsonParser;
import org.codehaus.jackson.map.MappingJsonFactory;

import java.io.*;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

/**
 * Tests for the compiler.
 * <p/>
 * User: sam
 * Date: May 3, 2010
 * Time: 10:23:54 AM
 */
public class CompilerTest extends TestCase {
  private File root;
  private Charset UTF8 = Charset.forName("UTF-8");

  public void testSimple() throws MustacheException, IOException, ExecutionException, InterruptedException {
    MustacheCompiler c = new MustacheCompiler(root);
    Mustache m = c.parseFile("simple.html");
    c.setOutputDirectory("target/classes");
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
  
  public void testSimpleWithSave() throws MustacheException, IOException, ExecutionException, InterruptedException {
    MustacheCompiler c = init();
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
    MustacheCompiler c = init();
    Mustache m = c.parseFile("simple.html");
    StringWriter sw = new StringWriter();
    FutureWriter writer = new FutureWriter(sw);
    m.execute(writer, new Scope(new Object() {
    }));
    writer.flush();
    assertEquals(getContents(root, "simplemissing.txt"), sw.toString());
  }

  public void testSetWriter() throws MustacheException, IOException, ExecutionException, InterruptedException {
    MustacheCompiler c = init();
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
    MustacheCompiler c = init();
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
    MustacheCompiler c = init();
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
    MustacheCompiler c = new MustacheCompiler(root);
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
    MustacheCompiler c = init();
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
    MustacheCompiler c = init();
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
    MustacheCompiler c = init();
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
    MustacheCompiler c = new MustacheCompiler(root);
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
    MustacheCompiler c = init();
    Mustache m = c.parseFile("template_partial.html");
    StringWriter sw = new StringWriter();
    FutureWriter writer = new FutureWriter(sw);
    m.execute(writer, scope);
    writer.flush();
    assertEquals(getContents(root, "template_partial.txt"), sw.toString());

  }

  public void testReadme() throws MustacheException, IOException {
    MustacheCompiler c = init();
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
    MustacheCompiler c = new MustacheCompiler(root);
    Mustache m = c.parseFile("items2.html");
    c.setOutputDirectory("target/classes");
    StringWriter sw = new StringWriter();
    FutureWriter writer = new FutureWriter(sw);
    long start = System.currentTimeMillis();
    m.execute(writer, new Scope(new Context()));
    writer.flush();
    long diff = System.currentTimeMillis() - start;
    assertTrue("Should be a little bit more than 1 second", diff > 1000 && diff < 2000);
    assertEquals(getContents(root, "items.txt"), sw.toString());
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
        final CallbackFuture<String> result = new CallbackFuture<String>();
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
    MustacheCompiler c = init();
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

  private MustacheCompiler init() {
    return new MustacheCompiler(root, "target/test-classes");
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
    root = new File("src/test/resources");
  }
}
