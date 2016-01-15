package com.github.mustachejava.functions;

import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.Mustache;
import com.github.mustachejava.MustacheException;
import com.github.mustachejava.MustacheFactory;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.*;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import static com.github.mustachejava.TestUtil.getContents;
import static org.junit.Assert.assertEquals;

public class TranslateBundleTest {

  private static File root;
  private static final String BUNDLE = "com.github.mustachejava.functions.translatebundle";

  @Test
  public void testTranslation() throws MustacheException, IOException, ExecutionException, InterruptedException {
    MustacheFactory c = new DefaultMustacheFactory(root);
    Mustache m = c.compile("translatebundle.html");
    StringWriter sw = new StringWriter();
    Map<String, Object> scope = new HashMap<>();
	  scope.put("trans", new TranslateBundleFunction(BUNDLE, Locale.US));
    m.execute(sw, scope);
    assertEquals(getContents(root, "translatebundle.txt"), sw.toString());
  }

  @BeforeClass
  public static void setUp() throws Exception {
    File compiler = new File("compiler").exists() ? new File("compiler") : new File(".");
    root = new File(compiler, "src/test/resources/functions");
  }
}
