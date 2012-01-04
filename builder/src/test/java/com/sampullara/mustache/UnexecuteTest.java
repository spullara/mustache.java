package com.sampullara.mustache;

import com.google.common.base.Function;

import com.sampullara.util.FutureWriter;
import com.sampullara.util.TemplateFunction;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.List;

import static junit.framework.Assert.assertEquals;

/**
 * Given a template and text, determine the data used to generate them.
 * <p/>
 * User: sam
 * Date: 9/5/11
 * Time: 10:49 AM
 */
public class UnexecuteTest {

  private static File root;

  @Test
  public void testReallySimpleUnexecute() throws MustacheException, IOException {
    MustacheJava c = init();
    Mustache m = c.parseFile("reallysimple.html");
    StringWriter sw = new StringWriter();
    m.execute(sw, new Scope(new Object() {
      String name = "Chris";
      int value = 10000;
    }));
    assertEquals(getContents(root, "reallysimple.txt"), sw.toString());

    Scope scope = m.unexecute(sw.toString());
    assertEquals("Chris", scope.get("name"));
    assertEquals("10000", scope.get("value"));
  }

  @Test
  public void testSimpleUnexecute() throws MustacheException, IOException {
    MustacheJava c = init();
    Mustache m = c.parseFile("unambiguoussimple.html");
    StringWriter sw = new StringWriter();
    m.execute(sw, new Scope(new Object() {
      String name = "Chris";
      int value = 10000;

      int taxed_value() {
        return (int) (this.value - (this.value * 0.4));
      }

      boolean in_ca = true;
    }));
    assertEquals(getContents(root, "unambiguoussimple.txt"), sw.toString());

    Scope scope = m.unexecute(sw.toString());
    assertEquals("Chris", scope.get("name"));
    assertEquals("10000", scope.get("value"));
    Scope in_ca = new Scope();
    in_ca.put("taxed_value", "6000");
    assertEquals(Arrays.asList(in_ca), scope.get("in_ca"));

    sw = new StringWriter();
    m.execute(sw, scope);
    assertEquals(getContents(root, "unambiguoussimple.txt"), sw.toString());
  }

  @Test
  public void testSimpleUnexecuteEncoded() throws MustacheException, IOException {
    MustacheJava c = init();
    Mustache m = c.parseFile("unambiguoussimple.html");
    StringWriter sw = new StringWriter();
    m.execute(sw, new Scope(new Object() {
      String name = "<Chris>";
      int value = 10000;

      int taxed_value() {
        return (int) (this.value - (this.value * 0.4));
      }

      boolean in_ca = true;
    }));
    assertEquals(getContents(root, "unambiguoussimpleencoded.txt"), sw.toString());

    Scope scope = m.unexecute(sw.toString());
    assertEquals("<Chris>", scope.get("name"));
    assertEquals("10000", scope.get("value"));
    Scope in_ca = new Scope();
    in_ca.put("taxed_value", "6000");
    assertEquals(Arrays.asList(in_ca), scope.get("in_ca"));

    sw = new StringWriter();
    m.execute(sw, scope);
    assertEquals(getContents(root, "unambiguoussimpleencoded.txt"), sw.toString());
  }

  @Test
  public void testComplexUnexecute() throws MustacheException, IOException {
    Scope scope = new Scope(new Object() {
      String header = "Colors";
      boolean include = true;
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

    MustacheJava c = init();
    Mustache m = c.parseFile("unexecutecomplex.html");
    StringWriter sw = new StringWriter();
    m.execute(sw, scope);
    assertEquals(getContents(root, "unexecutecomplex.txt"), sw.toString());

    scope = m.unexecute(sw.toString());
    sw = new StringWriter();
    m.execute(sw, scope);
    assertEquals(getContents(root, "unexecutecomplex.txt"), sw.toString());

    System.out.println(scope);
  }

  @Test
  public void testTemplateLamda() throws MustacheException, IOException {
    MustacheBuilder c = new MustacheBuilder(root);
    Mustache m = c.parseFile("unexecutetemplatelambda.html");
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
    assertEquals(getContents(root, "unexecutetemplatelambda.txt"), sw.toString());

    Scope scope = m.unexecute(sw.toString());
    sw = new StringWriter();
    m.execute(sw, scope);
    assertEquals(getContents(root, "unexecutetemplatelambda.txt"), sw.toString());

    System.out.println(scope);
  }

  @Test
  public void testPartial() throws MustacheException, IOException {
    MustacheBuilder c = init();
    Mustache m = c.parseFile("template_partial.html");
    StringWriter sw = new StringWriter();
    Scope scope = new Scope();
    scope.put("title", "Welcome");
    scope.put("template_partial_2", new Object() {
      String again = "Goodbye";
    });
    m.execute(sw, scope);
    assertEquals(getContents(root, "template_partial.txt"), sw.toString());

    scope = m.unexecute(sw.toString());
    sw = new StringWriter();
    m.execute(sw, scope);
    System.out.println(scope);
    assertEquals(getContents(root, "template_partial.txt"), sw.toString());
  }

  @Test
  public void testPartial2() throws MustacheException, IOException {
    MustacheBuilder c = init();
    Mustache m = c.parseFile("template_partial2.html");
    StringWriter sw = new StringWriter();
    Scope scope = new Scope();
    scope.put("title", "Welcome");
    scope.put("template_partial_2", new Object() {
      String again = "Goodbye";
    });
    scope.put("test", true);
    m.execute(sw, scope);
    assertEquals(getContents(root, "template_partial2.txt"), sw.toString());

    scope = m.unexecute(sw.toString());
    sw = new StringWriter();
    m.execute(sw, scope);
    System.out.println(scope);
    assertEquals(getContents(root, "template_partial2.txt"), sw.toString());
  }

  @Test
  public void testSimpleLamda() throws MustacheException, IOException {
    MustacheBuilder c = new MustacheBuilder(root);
    Mustache m = c.parseFile("explicitlambda.html");
    StringWriter sw = new StringWriter();
    m.execute(sw, new Scope(new Object() {
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
    assertEquals(getContents(root, "explicitlambda.txt"), sw.toString());

    Scope scope = m.unexecute(sw.toString());
    sw = new StringWriter();
    m.execute(sw, scope);
    System.out.println(scope);
    assertEquals(getContents(root, "explicitlambda.txt"), sw.toString());
  }

  @Test
  public void testIbis() throws MustacheException, IOException {
    MustacheBuilder c = new MustacheBuilder(root);
    Mustache m = c.parseFile("ibis.html");
    Scope unexecute = m.unexecute(getContents(root, "ibis-1107586786643355109.html"));
    StringWriter sw = new StringWriter();
    m.execute(sw, unexecute);
    assertEquals(getContents(root, "ibis-1107586786643355109.html"), sw.toString());
  }

  @Test
  public void testIbis2() throws MustacheException, IOException {
    MustacheBuilder c = new MustacheBuilder(new MustacheContext() {
      @Override
      public BufferedReader getReader(String name) throws MustacheException {
        String path = name;
        if (name.startsWith("dir:")) {
          path = "network_digest_v1/" + name.split(":")[1];
        } else if (name.startsWith("modules:")) {
          path = "modules/" + name.split(":")[1];
        }
        try {
          return new BufferedReader(new FileReader(new File(root, "ibis2/" + path)));
        } catch (Exception e) {
          throw new MustacheException("Failed to open: " + path, e);
        }
      }
    });
    Mustache m = c.parseFile("content.html.mustache");
    Scope unexecute = m.unexecute(getContents(root, "ibis2/test.html"));
    StringWriter sw = new StringWriter();
    m.execute(sw, unexecute);
    assertEquals(getContents(root, "ibis2/test.html"), sw.toString());
    System.out.println(unexecute);
  }

  private MustacheBuilder init() {
    return new MustacheBuilder(root);
  }

  protected String getContents(File root, String file) throws IOException {
    BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(new File(root, file)), "UTF-8"));
    StringWriter capture = new StringWriter();
    char[] buffer = new char[8192];
    int read;
    while ((read = br.read(buffer)) != -1) {
      capture.write(buffer, 0, read);
    }
    return capture.toString();
  }

  @BeforeClass
  public static void setUp() throws Exception {
    File file = new File("src/test/resources");
    root = new File(file, "simple.html").exists() ? file : new File("../src/test/resources");
  }

}
