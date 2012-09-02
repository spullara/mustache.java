package com.github.mustachejava.functions;

import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.Mustache;
import com.github.mustachejava.MustacheException;
import com.github.mustachejava.MustacheFactory;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.*;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import static org.junit.Assert.assertEquals;

public class CommentTest {

  private static File root;

  @Test
  public void testCommentBlock() throws MustacheException, IOException, ExecutionException, InterruptedException {
    MustacheFactory c = new DefaultMustacheFactory(root);
    Mustache m = c.compile("comment.html");
    StringWriter sw = new StringWriter();
    Map scope = new HashMap();
    scope.put("ignored", "ignored");
    m.execute(sw, scope);
    assertEquals(getContents(root, "comment.txt"), sw.toString());
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
    File file = new File("compiler/src/test/resources/functions");
    root = new File(file, "comment.html").exists() ? file : new File("src/test/resources/functions");
  }
}
