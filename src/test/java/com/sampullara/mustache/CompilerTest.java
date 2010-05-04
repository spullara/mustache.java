package com.sampullara.mustache;

import junit.framework.TestCase;

import java.io.File;
import java.io.PrintWriter;
import java.util.ArrayList;

/**
 * Tests for the compiler.
 * <p/>
 * User: sam
 * Date: May 3, 2010
 * Time: 10:23:54 AM
 */
public class CompilerTest extends TestCase {
  public void testSimple() throws MustacheException {
    Compiler c = new Compiler(new File("src/test/resources"));
    Mustache m = c.parseFile("simple.html");
    PrintWriter writer = new PrintWriter(System.out);
    m.execute(writer, new Scope(new Object() {
      String name = "world";
      String value = "6000";
      String taxed_value = "3600";
      boolean in_ca = true;
    }));
    m.execute(writer, new Scope(new Object() {
      String name = "world";
      String value = "6000";
      String taxed_value = "3600";
      boolean in_ca = false;
    }));
    writer.flush();
  }

  public void testEscaped() throws MustacheException {
    Compiler c = new Compiler(new File("src/test/resources"));
    Mustache m = c.parseFile("escaped.html");
    PrintWriter writer = new PrintWriter(System.out);
    m.execute(writer, new Scope(new Object() {
      String title = "Bear > Shark";
      String entities = "&quot;";
    }));
    writer.flush();
  }

  public void testUnescaped() throws MustacheException {
    Compiler c = new Compiler(new File("src/test/resources"));
    Mustache m = c.parseFile("unescaped.html");
    PrintWriter writer = new PrintWriter(System.out);
    m.execute(writer, new Scope(new Object() {
      String title() { return "Bear > Shark"; }
    }));
    writer.flush();
  }

  public void testInverted() throws MustacheException {
    Compiler c = new Compiler(new File("src/test/resources"));
    Mustache m = c.parseFile("inverted_section.html");
    PrintWriter writer = new PrintWriter(System.out);
    m.execute(writer, new Scope(new Object() {
      String name() { return "Bear > Shark"; }
      ArrayList repo = new ArrayList();
    }));
    writer.flush();
  }
}
