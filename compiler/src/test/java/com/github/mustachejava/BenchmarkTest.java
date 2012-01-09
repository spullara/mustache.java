package com.github.mustachejava;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.List;

import com.github.mustachejava.impl.DefaultCodeFactory;
import junit.framework.TestCase;

/**
 * Compare compilation with interpreter.
 * <p/>
 * User: sam
 * Date: 5/14/11
 * Time: 9:28 PM
 */
public class BenchmarkTest extends TestCase {
  private static final int TIME = 2000;
  private File root;

  protected void setUp() throws Exception {
    super.setUp();
    File file = new File("src/test/resources");
    root = new File(file, "simple.html").exists() ? file : new File("../src/test/resources");
  }

  public void testComplex() throws MustacheException, IOException {
    System.out.println("complex.html evaluations per millisecond:");
    for (int i = 0; i < 3; i++) {
      {
        MustacheCompiler c = new MustacheCompiler(new DefaultCodeFactory());
        Mustache m = c.compile("complex.html");
        complextest(m);
        long start = System.currentTimeMillis();
        int total = 0;
        while (true) {
          complextest(m);
          total++;
          if (System.currentTimeMillis() - start > TIME) break;
        }
        System.out.println("Interpreted: " + total/TIME);
      }
    }
  }

  private StringWriter complextest(Mustache m) throws MustacheException, IOException {
    StringWriter sw = new StringWriter();
    m.execute(sw, Arrays.asList((Object)new ComplexObject()));
    return sw;
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
}
