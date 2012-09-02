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

import static org.junit.Assert.assertEquals;

public class TranslateBundleTest {

  private static File root;
  private static final String BUNDLE = "com.github.mustachejava.functions.translatebundle";

  @Test
  public void testTranslation() throws MustacheException, IOException, ExecutionException, InterruptedException {
    MustacheFactory c = new DefaultMustacheFactory(root);
    Mustache m = c.compile("translatebundle.html");
    StringWriter sw = new StringWriter();
    Map<String, Object> scope = new HashMap<String, Object>();
	  scope.put("trans", new TranslateBundleFunction(BUNDLE, Locale.US));
    m.execute(sw, scope);
    assertEquals(getContents(root, "translatebundle.txt"), sw.toString());
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

  @BeforeClass
  public static void setUp() throws Exception {
    File compiler = new File("compiler").exists() ? new File("compiler") : new File(".");
    root = new File(compiler, "src/test/resources/functions");
  }
}
