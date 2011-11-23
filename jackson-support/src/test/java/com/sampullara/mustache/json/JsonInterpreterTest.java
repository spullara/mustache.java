package com.sampullara.mustache.json;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import com.google.common.collect.ImmutableMap;

import com.sampullara.mustache.Mustache;
import com.sampullara.mustache.MustacheBuilder;
import com.sampullara.mustache.MustacheException;
import com.sampullara.mustache.Scope;
import com.sampullara.util.FutureWriter;
import com.sampullara.util.http.JSONHttpRequest;
import junit.framework.TestCase;
import org.codehaus.jackson.JsonFactory;
import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.JsonParser;
import org.codehaus.jackson.map.MappingJsonFactory;

/**
 * Tests for the compiler.
 * <p/>
 * User: sam
 * Date: May 3, 2010
 * Time: 10:23:54 AM
 */
public class JsonInterpreterTest extends TestCase {
  private File root;

 public void testSimpleWithJson() throws MustacheException, IOException, ExecutionException, InterruptedException {
    MustacheBuilder c = init();
    Mustache m = c.parseFile("simple.html");
    StringWriter sw = new StringWriter();
    JsonFactory jf = new MappingJsonFactory();
    JsonNode jsonNode = jf.createJsonParser("{\"name\":\"Chris\", \"value\":10000, \"taxed_value\":6000,\"in_ca\":true}").readValueAsTree();
    m.execute(sw, jsonNode);
    assertEquals(getContents(root, "simple.txt"), sw.toString());
  }

  public void testSimpleWithJsonAndWriter() throws MustacheException, IOException, ExecutionException, InterruptedException {
    MustacheBuilder c = init();
    Mustache m = c.parseFile("simple.html");
    StringWriter sw = new StringWriter();
    FutureWriter writer = new FutureWriter(sw);
    JsonFactory jf = new MappingJsonFactory();
    JsonNode jsonNode = jf.createJsonParser("{\"name\":\"Chris\", \"value\":10000, \"taxed_value\":6000,\"in_ca\":true}").readValueAsTree();
    m.execute(writer, jsonNode);
    writer.flush();
    assertEquals(getContents(root, "simple.txt"), sw.toString());
  }

  public void testJson() throws IOException, MustacheException {
    String content = getContents(root, "template_partial.js");
    content = content.substring(content.indexOf("=") + 1);
    JsonParser jp = new MappingJsonFactory().createJsonParser(content);
    JsonNode jsonNode = jp.readValueAsTree();
    Scope scope = new Scope(jsonNode);
    MustacheBuilder c = init();
    Mustache m = c.parseFile("template_partial.html");
    StringWriter sw = new StringWriter();
    FutureWriter writer = new FutureWriter(sw);
    m.execute(writer, scope);
    writer.flush();
    assertEquals(getContents(root, "template_partial.txt"), sw.toString());

  }

  public void testJSONHttpRequest() throws MustacheException, IOException {
    MustacheBuilder c = init();
    Mustache m = c.parseFile("simple2.html");
    StringWriter sw = new StringWriter();
    FutureWriter writer = new FutureWriter(sw);
    m.execute(writer, new Scope(new Object() {
      Future<JsonNode> data() throws IOException {
        JSONHttpRequest jhr = new JSONHttpRequest("http://www.javarants.com/simple.json");
        return jhr.execute();
      }
    }));
    writer.flush();
    assertEquals(getContents(root, "simple.txt"), sw.toString());
  }

  public void testXSS() throws MustacheException, IOException, ExecutionException, InterruptedException {
    MustacheBuilder c = init();
    Mustache m = c.parseFile("xss.html");
    final StringWriter json = new StringWriter();
    ImmutableMap<String, Object> of = ImmutableMap.<String, Object>of("foo", "bar", "\"baz\"", 42);
    MappingJsonFactory jf = new MappingJsonFactory();
    JsonGenerator jg = jf.createJsonGenerator(json);
    jg.writeObject(of);
    jg.flush();
    StringWriter sw = new StringWriter();
    FutureWriter writer = new FutureWriter(sw);
    m.execute(writer, new Scope(new Object() {
      String message = "I <3 Ponies!";
      String object = json.toString();
    }));
    writer.flush();
    assertEquals(getContents(root, "xss.txt"), sw.toString());
  }

  private MustacheBuilder init() {
    Scope.setDefaultObjectHandler(new JsonObjectHandler());
    return new MustacheBuilder(root);
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

  protected void setUp() throws Exception {
    super.setUp();
    File file = new File("src/test/resources");
    root = new File(file, "simple.html").exists() ? file : new File("../src/test/resources");
  }

}
