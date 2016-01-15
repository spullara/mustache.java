package com.github.mustachejava;

import org.junit.BeforeClass;
import org.junit.Test;

import java.io.*;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import static com.github.mustachejava.TestUtil.getContents;
import static org.junit.Assert.assertEquals;

public class ExtensionTest {

  private static File root;

  @Test
  public void testSub() throws MustacheException, IOException, ExecutionException, InterruptedException {
    MustacheFactory c = new DefaultMustacheFactory(root);
    Mustache m = c.compile("sub.html");
    StringWriter sw = new StringWriter();
    Map scope = new HashMap();
    scope.put("name", "Sam");
    scope.put("randomid", "asdlkfj");
    m.execute(sw, scope);
    assertEquals(getContents(root, "sub.txt"), sw.toString());
  }

  @Test
  public void testSubInPartial() throws MustacheException, IOException, ExecutionException, InterruptedException {
    MustacheFactory c = new DefaultMustacheFactory(root);
    Mustache m = c.compile("partialsub.html");
    StringWriter sw = new StringWriter();
    Map scope = new HashMap();
    scope.put("name", "Sam");
    scope.put("randomid", "asdlkfj");
    m.execute(sw, scope);
    assertEquals(getContents(root, "sub.txt"), sw.toString());
  }

  @Test
  public void testPartialInSub() throws MustacheException, IOException, ExecutionException, InterruptedException {
    MustacheFactory c = new DefaultMustacheFactory(root);
    Mustache m = c.compile("partialsubpartial.html");
    StringWriter sw = new StringWriter();
    Map scope = new HashMap();
    scope.put("randomid", "asdlkfj");
    m.execute(sw, scope);
    assertEquals(getContents(root, "partialsubpartial.txt"), sw.toString());
  }

  @Test
  public void testFollow() throws MustacheException, IOException, ExecutionException, InterruptedException {
    MustacheFactory c = new DefaultMustacheFactory(root);
    Mustache m = c.compile("follownomenu.html");
    StringWriter sw = new StringWriter();
    m.execute(sw, new Object());
    assertEquals(getContents(root, "follownomenu.txt"), sw.toString());
  }

  @Test
  public void testMultipleExtensions() throws MustacheException, IOException, ExecutionException, InterruptedException {
    MustacheFactory c = new DefaultMustacheFactory(root);
    Mustache m = c.compile("multipleextensions.html");
    StringWriter sw = new StringWriter();
    m.execute(sw, new Object());
    assertEquals(getContents(root, "multipleextensions.txt"), sw.toString());
  }

  @Test
  public void testParentReplace() throws MustacheException, IOException, ExecutionException, InterruptedException {
    MustacheFactory c = new DefaultMustacheFactory(root);
    Mustache m = c.compile("replace.html");
    StringWriter sw = new StringWriter();
    m.execute(sw, new Object() { String replace = "false"; });
    assertEquals(getContents(root, "replace.txt"), sw.toString());
  }

  @Test
  public void testSubBlockCaching() throws MustacheException, IOException, ExecutionException, InterruptedException {
    MustacheFactory c = new DefaultMustacheFactory(root);
    Mustache m = c.compile("subblockchild1.html");
    StringWriter sw = new StringWriter();
    m.execute(sw, new Object() {});
    assertEquals(getContents(root, "subblockchild1.txt"), sw.toString());
    
    m = c.compile("subblockchild2.html");
    sw = new StringWriter();
    m.execute(sw,  new Object() {});
    assertEquals(getContents(root, "subblockchild2.txt"), sw.toString());
    
    m = c.compile("subblockchild1.html");
    sw = new StringWriter();
    m.execute(sw, new Object() {});
    assertEquals(getContents(root, "subblockchild1.txt"), sw.toString());
  }

  @Test
  public void testSubSub() throws MustacheException, IOException, ExecutionException, InterruptedException {
    MustacheFactory c = new DefaultMustacheFactory(root);
    Mustache m = c.compile("subsub.html");
    StringWriter sw = new StringWriter();
    Map scope = new HashMap();
    scope.put("name", "Sam");
    scope.put("randomid", "asdlkfj");
    m.execute(sw, scope);
    assertEquals(getContents(root, "subsub.txt"), sw.toString());
  }

  @Test
  public void testClientMethod() throws MustacheException, IOException, ExecutionException, InterruptedException {
    MustacheFactory c = new DefaultMustacheFactory(root);
    Mustache m = c.compile("client.html");
    StringWriter sw = new StringWriter();
    Map scope = new HashMap();
    scope.put("reply", "TestReply");
    scope.put("commands", Arrays.asList("a", "b"));
    m.execute(sw, scope);
    assertEquals(getContents(root, "client.txt"), sw.toString());
  }

  @Test
  public void testSubSubCaching() throws MustacheException, IOException, ExecutionException, InterruptedException {
    MustacheFactory c = new DefaultMustacheFactory(root);
    Mustache m = c.compile("subsubchild1.html");
    StringWriter sw = new StringWriter();
    m.execute(sw, new Object() {});
    assertEquals(getContents(root, "subsubchild1.txt"), sw.toString());
    
    m = c.compile("subsubchild2.html");
    sw = new StringWriter();
    m.execute(sw,  new Object() {});
    assertEquals(getContents(root, "subsubchild2.txt"), sw.toString());
  }

  @Test
  public void testSubSubCaching2() throws MustacheException, IOException, ExecutionException, InterruptedException {
    MustacheFactory c = new DefaultMustacheFactory(root);
    Mustache m = c.compile("subsubchild1.html");
    StringWriter sw = new StringWriter();
    m.execute(sw, new Object() {});
    assertEquals(getContents(root, "subsubchild1.txt"), sw.toString());
    
    m = c.compile("subsubchild3.html");
    sw = new StringWriter();
    m.execute(sw,  new Object() {});
    assertEquals(getContents(root, "subsubchild3.txt"), sw.toString());
  }

  @Test
  public void testNested() throws IOException {
    MustacheFactory c = new DefaultMustacheFactory(root);
    Mustache m = c.compile("nested_inheritance.html");
    StringWriter sw = new StringWriter();
    m.execute(sw, new Object() {});
    assertEquals(getContents(root, "nested_inheritance.txt"), sw.toString());
  }

  @BeforeClass
  public static void setUp() throws Exception {
    File file = new File("compiler/src/test/resources");
    root = new File(file, "sub.html").exists() ? file : new File("src/test/resources");
  }


}
