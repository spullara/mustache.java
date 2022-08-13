package com.github.mustachejava;

import org.junit.BeforeClass;
import org.junit.Test;

import java.io.*;
import java.util.HashMap;
import java.util.Map;

import static com.github.mustachejava.TestUtil.getContents;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

public class FallbackTest {

  private static File rootDefault;
  private static File rootOverride;

  @Test
  public void testDefaultPage1() throws MustacheException, IOException {
    MustacheFactory c = new FallbackMustacheFactory(rootDefault, rootDefault);  // no override
    Mustache m = c.compile("page1.html");
    StringWriter sw = new StringWriter();
    Map scope = new HashMap();
    scope.put("title", "My title");
    m.execute(sw, scope);
    assertEquals(getContents(rootDefault, "page1.txt"), sw.toString());
  }

  @Test
  public void testOverridePage1() throws MustacheException, IOException {
    MustacheFactory c = new FallbackMustacheFactory(rootOverride, rootDefault);
    Mustache m = c.compile("page1.html");
    StringWriter sw = new StringWriter();
    Map scope = new HashMap();
    scope.put("title", "My title");
    m.execute(sw, scope);
    assertEquals(getContents(rootOverride, "page1.txt"), sw.toString());
  }

  @Test
  public void testOverridePage2() throws MustacheException, IOException {
    MustacheFactory c = new FallbackMustacheFactory(rootOverride, rootDefault);
    Mustache m = c.compile("page2.html");
    StringWriter sw = new StringWriter();
    Map scope = new HashMap();
    scope.put("title", "My title");
    m.execute(sw, scope);
    assertEquals(getContents(rootOverride, "page2.txt"), sw.toString());
  }

  @Test
  public void testMustacheNotFoundException() {
    String nonExistingMustache = "404";
    MustacheNotFoundException e = assertThrows(MustacheNotFoundException.class, () -> {
      MustacheFactory c = new FallbackMustacheFactory(rootOverride, rootDefault);
      c.compile(nonExistingMustache);
    });
    assertEquals(nonExistingMustache, e.getName());
  }

  @BeforeClass
  public static void setUp() throws Exception {
    File file = new File("compiler/src/test/resources/fallback/default");
    rootDefault = new File(file, "base.html").exists() ? file : new File("src/test/resources/fallback/default");
    file = new File("compiler/src/test/resources/fallback/override");
    rootOverride = new File(file, "base.html").exists() ? file : new File("src/test/resources/fallback/override");
  }
}
