package com.github.mustachejava.inverter;

import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.Mustache;
import com.github.mustachejava.util.Node;
import org.junit.Test;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicInteger;

import static com.github.mustachejava.util.NodeValue.list;
import static com.github.mustachejava.util.NodeValue.value;
import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;

public class InverterTest {

  @Test
  public void testParser() throws IOException {
    DefaultMustacheFactory dmf = new DefaultMustacheFactory();
    Mustache compile = dmf.compile("fdbcli.mustache");
    Path file = FileSystems.getDefault().getPath("src/test/resources/fdbcli.txt");
    String txt = new String(Files.readAllBytes(file), "UTF-8");
    Node invert = compile.invert(new Node(), txt, new AtomicInteger(0));
    System.out.println(invert);
  }

  @Test
  public void testSimple() throws IOException {
    DefaultMustacheFactory dmf = new DefaultMustacheFactory();
    Mustache test = dmf.compile(new StringReader("test {{value}} test"), "test");
    Node invert = test.invert(new Node(), "test value test", new AtomicInteger(0));
    Node node = new Node();
    node.put("value", value("value"));
    assertEquals(node, invert);
  }

  @Test
  public void testIterable() throws IOException {
    DefaultMustacheFactory dmf = new DefaultMustacheFactory();
    Mustache test = dmf.compile(new StringReader("{{#values}}\ntest: {{value}}\n{{/values}}"), "test");
    Node invert = test.invert(new Node(), "test: sam\ntest: fred\n", new AtomicInteger(0));
    Node node = new Node();
    Node sam = new Node();
    sam.put("value", value("sam"));
    Node fred = new Node();
    fred.put("value", value("sam"));
    node.put("values", list(asList(sam, fred)));
    assertEquals(node, invert);
    StringWriter sw = new StringWriter();
    test.execute(sw, invert).close();
    System.out.println(sw);
  }
}
