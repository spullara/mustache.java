package com.sampullara.mustache;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.util.concurrent.ExecutionException;

import com.sampullara.util.FutureWriter;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class ExtensionTest {

  private static File root;

  @Test
  public void testSub() throws MustacheException, IOException, ExecutionException, InterruptedException {
    MustacheBuilder c = new MustacheBuilder(root);
    Mustache m = c.parseFile("sub.html");
    StringWriter sw = new StringWriter();
    FutureWriter writer = new FutureWriter(sw);
    m.execute(writer, new Scope());
    writer.flush();
    assertEquals(getContents(root, "sub.txt"), sw.toString());
  }

  @Test
  public void testTooMany() throws MustacheException, IOException, ExecutionException, InterruptedException {
    MustacheBuilder c = new MustacheBuilder(root);
    try {
      Mustache m = c.parseFile("toomany.html");
      StringWriter sw = new StringWriter();
      FutureWriter writer = new FutureWriter(sw);
      m.execute(writer, new Scope());
      writer.flush();
      fail("Should fail");
    } catch (Exception e) {
    }
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
    System.setProperty("mustache.debug", "true");
    File file = new File("src/test/resources");
    root = new File(file, "sub.html").exists() ? file : new File("../src/test/resources");
  }


}
