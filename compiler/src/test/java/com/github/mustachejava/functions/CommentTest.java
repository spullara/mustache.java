package com.github.mustachejava.functions;

import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.Mustache;
import com.github.mustachejava.MustacheException;
import com.github.mustachejava.MustacheFactory;

import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import static com.github.mustachejava.TestUtil.getContents;
import static org.junit.Assert.assertEquals;

public class CommentTest {

  @Test
  public void testCommentBlock() throws MustacheException, IOException, ExecutionException, InterruptedException {
    File root = getRoot("comment.html");
    MustacheFactory c = new DefaultMustacheFactory(root);
    Mustache m = c.compile("comment.html");
    StringWriter sw = new StringWriter();
    Map scope = new HashMap();
    scope.put("ignored", "ignored");
    m.execute(sw, scope);
    assertEquals(getContents(root, "comment.txt"), sw.toString());
  }

  @Test
  public void testCommentInline() throws MustacheException, IOException, ExecutionException, InterruptedException {
    File root = getRoot("commentinline.html");
    MustacheFactory c = new DefaultMustacheFactory(root);
    Mustache m = c.compile("commentinline.html");
    StringWriter sw = new StringWriter();
    Map scope = new HashMap();
    scope.put("title", "A Comedy of Errors");
    m.execute(sw, scope);
    assertEquals(getContents(root, "commentinline.txt"), sw.toString());
  }

  @Test
  public void testInlineCommentWithinExtendCodeBlock() throws MustacheException, IOException, ExecutionException, InterruptedException {
    File root = getRoot("commentWithinExtendCodeBlock.html");
    MustacheFactory c = new DefaultMustacheFactory(root);
    Mustache m = c.compile("commentWithinExtendCodeBlock.html");
    StringWriter sw = new StringWriter();
    m.execute(sw, Collections.emptyList());
    assertEquals(getContents(root, "commentWithinExtendCodeBlock.txt"), sw.toString());
  }

  private File getRoot(String fileName) {
    File file = new File("compiler/src/test/resources/functions");
    return new File(file, fileName).exists() ? file : new File("src/test/resources/functions");
  }
}
