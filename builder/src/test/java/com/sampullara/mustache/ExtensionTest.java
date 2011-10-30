package com.sampullara.mustache;

import com.sampullara.util.FutureWriter;
import junit.framework.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.util.concurrent.ExecutionException;

import static org.junit.Assert.assertEquals;

public class ExtensionTest {

  private static File root;

  @Test
  public void testSub() throws MustacheException, IOException, ExecutionException, InterruptedException {
    MustacheBuilder c = new MustacheBuilder(root);
    Mustache m = c.parseFile("sub.html");
    StringWriter sw = new StringWriter();
    FutureWriter writer = new FutureWriter(sw);
    Scope scope = new Scope();
    scope.put("name", "Sam");
    scope.put("randomid", "asdlkfj");
    m.execute(writer, scope);
    writer.flush();
    assertEquals(getContents(root, "sub.txt"), sw.toString());

    scope = m.unexecute(sw.toString());
    sw = new StringWriter();
    m.execute(sw, scope);
    System.out.println(scope);
    Assert.assertEquals(getContents(root, "sub.txt"), sw.toString());
  }

  @Test
  public void testSubSub() throws MustacheException, IOException, ExecutionException, InterruptedException {
    MustacheBuilder c = new MustacheBuilder(root);
    Mustache m = c.parseFile("subsub.html");
    StringWriter sw = new StringWriter();
    FutureWriter writer = new FutureWriter(sw);
    Scope scope = new Scope();
    scope.put("name", "Sam");
    scope.put("randomid", "asdlkfj");
    m.execute(writer, scope);
    writer.flush();
    assertEquals(getContents(root, "subsub.txt"), sw.toString());

    scope = m.unexecute(sw.toString());
    sw = new StringWriter();
    m.execute(sw, scope);
    System.out.println(scope);
    Assert.assertEquals(getContents(root, "subsub.txt"), sw.toString());
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
    File file = new File("src/test/resources");
    root = new File(file, "sub.html").exists() ? file : new File("../src/test/resources");
  }


}
