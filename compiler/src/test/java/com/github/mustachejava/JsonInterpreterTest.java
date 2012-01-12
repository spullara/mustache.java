package com.github.mustachejava;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.google.common.io.CharStreams;

import junit.framework.TestCase;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.MappingJsonFactory;

/**
 * Tests for the compiler.
 * <p/>
 * User: sam
 * Date: May 3, 2010
 * Time: 10:23:54 AM
 */
public class JsonInterpreterTest extends TestCase {
  private static final int TIME = 5;

  private File root;

  public Object toObject(final JsonNode node) {
    if (node.isArray()) {
      return new ArrayList() {{
        for (JsonNode jsonNodes : node) {
          add(toObject(jsonNodes));
        }
      }};
    } else if (node.isObject()) {
      return new HashMap() {{
        for (Iterator<Map.Entry<String,JsonNode>> i = node.getFields(); i.hasNext(); ) {
          Map.Entry<String, JsonNode> next = i.next();
          Object o = toObject(next.getValue());
          put(next.getKey(), o);
        }
      }};
    } else if (node.isNull()) {
      return null;
    } else {
      return node.asText();
    }
  }

  public void testHogan() throws MustacheException, IOException {
    MustacheFactory mb = new DefaultMustacheFactory(root);
    Mustache parse = mb.compile("timeline.mustache");
    long start = System.currentTimeMillis();
    mb.compile("timeline.mustache");
    System.out.println(System.currentTimeMillis() - start);
    start = System.currentTimeMillis();
    MappingJsonFactory jf = new MappingJsonFactory();
    InputStream json = getClass().getClassLoader().getResourceAsStream("hogan.json");
    final Map node = (Map) toObject(jf.createJsonParser(json).readValueAsTree());
    System.out.println(node);
    Object parent = new Object() {
      int uid = 0;
      List tweets = new ArrayList() {{
        for (int i = 0; i < 50; i++) {
          add(node);
        }
      }};
    };
    StringWriter writer = new StringWriter();
    parse.execute(writer, parent);

    start = System.currentTimeMillis();
    for (int i = 0; i < 500; i++) {
      parse.execute(new StringWriter(), parent);
    }
    System.out.println((System.currentTimeMillis() - start));

    start = System.currentTimeMillis();
    for (int i = 0; i < 500; i++) {
      parse.execute(new StringWriter(), parent);
    }
    System.out.println((System.currentTimeMillis() - start));

    System.out.println("timeline.html evaluations per millisecond:");
    for (int i = 0; i < 2; i++) {
      {
        start = System.currentTimeMillis();
        int total = 0;
        while (true) {
          parse.execute(new NullWriter(), parent);
          total++;
          if (System.currentTimeMillis() - start > TIME*1000) break;
        }
        System.out.println("Serial: " + total/TIME + "/s");
      }
      {
        start = System.currentTimeMillis();
        int total = 0;
        while (true) {
          parse.execute(new StringWriter(), parent);
          total++;
          if (System.currentTimeMillis() - start > TIME*1000) break;
        }
        System.out.println("Serial: " + total/TIME + "/s");
      }
    }
  }

  static class NullWriter extends Writer {
    @Override
    public void write(int c) throws IOException {
    }

    @Override
    public void write(char[] cbuf) throws IOException {
    }

    @Override
    public void write(String str) throws IOException {
    }

    @Override
    public void write(String str, int off, int len) throws IOException {
    }

    @Override
    public Writer append(CharSequence csq) throws IOException {
      return this;
    }

    @Override
    public Writer append(CharSequence csq, int start, int end) throws IOException {
      return this;
    }

    @Override
    public Writer append(char c) throws IOException {
      return this;
    }

    @Override
    public void write(char[] cbuf, int off, int len) throws IOException {
    }

    @Override
    public void flush() throws IOException {
    }

    @Override
    public void close() throws IOException {
    }
  }

  protected void setUp() throws Exception {
    super.setUp();
    File file = new File("src/test/resources");
    root = new File(file, "simple.html").exists() ? file : new File("../src/test/resources");
  }

}
