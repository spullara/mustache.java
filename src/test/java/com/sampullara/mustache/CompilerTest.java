package com.sampullara.mustache;

import junit.framework.TestCase;

import java.io.*;
import java.util.ArrayList;

/**
 * Tests for the compiler.
 * <p/>
 * User: sam
 * Date: May 3, 2010
 * Time: 10:23:54 AM
 */
public class CompilerTest extends TestCase {
  private File root;

  public void testSimple() throws MustacheException, IOException {
    Compiler c = new Compiler(root);
    Mustache m = c.parseFile("simple.html");
    StringWriter writer = new StringWriter();
    m.execute(writer, new Scope(new Object() {
      String name = "Chris";
      int value = 10000;
      int taxed_value() {
        return (int) (this.value - (this.value * 0.4));
      }
      boolean in_ca = true;
    }));
    assertEquals(getContents(root, "simple.txt"), writer.toString());
    writer = new StringWriter();
    m.execute(writer, new Scope(new Object() {
      String name = "Chris";
      int value = 10000;
      int taxed_value() {
        return (int) (this.value - (this.value * 0.4));
      }
      boolean in_ca = false;
    }));
    writer.flush();
    assertEquals(getContents(root, "simple2.txt"), writer.toString());
  }

  public void testEscaped() throws MustacheException, IOException {
    Compiler c = new Compiler(root);
    Mustache m = c.parseFile("escaped.html");
    StringWriter writer = new StringWriter();
    m.execute(writer, new Scope(new Object() {
      String title = "Bear > Shark";
      String entities = "&quot;";
    }));
    writer.flush();
    assertEquals(getContents(root, "escaped.txt"), writer.toString());
  }

  public void testUnescaped() throws MustacheException, IOException {
    Compiler c = new Compiler(root);
    Mustache m = c.parseFile("unescaped.html");
    StringWriter writer = new StringWriter();
    m.execute(writer, new Scope(new Object() {
      String title() { return "Bear > Shark"; }
    }));
    writer.flush();
    assertEquals(getContents(root, "unescaped.txt"), writer.toString());
  }

  public void testInverted() throws MustacheException, IOException {
    Compiler c = new Compiler(root);
    Mustache m = c.parseFile("inverted_section.html");
    StringWriter writer = new StringWriter();
    m.execute(writer, new Scope(new Object() {
      String name() { return "Bear > Shark"; }
      ArrayList repo = new ArrayList();
    }));
    writer.flush();
    assertEquals(getContents(root, "inverted_section.txt"), writer.toString());
  }

  public void testComments() throws MustacheException, IOException {
    Compiler c = new Compiler(root);
    Mustache m = c.parseFile("comments.html");
    StringWriter writer = new StringWriter();
    m.execute(writer, new Scope(new Object() {
      String title() { return "A Comedy of Errors"; }
    }));
    writer.flush();
    assertEquals(getContents(root, "comments.txt"), writer.toString());
  }

  private String getContents(File root, String file) throws IOException {
    BufferedReader br = new BufferedReader(new FileReader(new File(root, file)));
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
