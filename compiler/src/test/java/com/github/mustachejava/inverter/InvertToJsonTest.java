package com.github.mustachejava.inverter;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.MappingJsonFactory;
import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.Mustache;
import com.github.mustachejava.util.Node;
import com.github.mustachejava.util.NodeValue;
import org.junit.Test;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

public class InvertToJsonTest extends InvertUtils {
  @Test
  public void testToJson() throws IOException {
    DefaultMustacheFactory dmf = new DefaultMustacheFactory();
    Mustache compile = dmf.compile("fdbcli.mustache");
    Path file = getPath("src/test/resources/fdbcli.txt");
    String txt = new String(Files.readAllBytes(file), "UTF-8");
    Node invert = compile.invert(txt);

    output(invert);
  }

  @Test
  public void testToJson2() throws IOException {
    DefaultMustacheFactory dmf = new DefaultMustacheFactory();
    Mustache compile = dmf.compile("fdbcli2.mustache");
    Path file = getPath("src/test/resources/fdbcli2.txt");
    String txt = new String(Files.readAllBytes(file), "UTF-8");
    Node invert = compile.invert(txt);

    output(invert);
  }

  @Test
  public void testToJson3() throws IOException {
    DefaultMustacheFactory dmf = new DefaultMustacheFactory();
    Mustache compile = dmf.compile("psauxwww.mustache");
    Path file = getPath("src/test/resources/psauxwww.txt");
    String txt = new String(Files.readAllBytes(file), "UTF-8");
    Node invert = compile.invert(txt);

    output(invert);
  }

  @Test
  public void testToJson4() throws IOException {
    DefaultMustacheFactory dmf = new DefaultMustacheFactory();
    Mustache compile = dmf.compile("fdbcli2.mustache");
    Path file = getPath("src/test/resources/fdbcli3.txt");
    String txt = new String(Files.readAllBytes(file), "UTF-8");
    System.out.println("Input text:[");
    System.out.print(txt);
    System.out.println("]");
    Node invert = compile.invert(txt);

    output(invert);
  }

  @Test
  public void testToJson5() throws IOException {
    DefaultMustacheFactory dmf = new DefaultMustacheFactory();
    Mustache compile = dmf.compile("fdbcli3.mustache");
    Path file = getPath("src/test/resources/fdbcli.txt");
    String txt = new String(Files.readAllBytes(file), "UTF-8");
    Node invert = compile.invert(txt);

    output(invert);
  }

  private void output(Node invert) throws IOException {
    MappingJsonFactory jf = new MappingJsonFactory();
    StringWriter out = new StringWriter();
    JsonGenerator jg = jf.createJsonGenerator(out);
    writeNode(jg, invert);
    jg.flush();
    System.out.println(out.toString());
  }

  @Test
  public void testDiskstats() throws IOException {
    DefaultMustacheFactory dmf = new DefaultMustacheFactory();
    Mustache m = dmf.compile(new StringReader("{{#disk}}\n" +
            "\\s+[0-9]+\\s+[0-9]+\\s+{{tag_device}} {{reads}} {{reads_merged}} {{sectors_read}} {{read_time}} {{writes}} {{writes_merged}} {{sectors_written}} {{write_time}} {{ios}} {{io_time}} {{weighted_io_time}}\n" +
            "{{/disk}}"), "diskstats");
    String txt = "  220   100 xvdb 3140 43 23896 216 57698654 45893891 1261011016 12232816 0 10994276 12222124\n" +
            "  220   100  xvdk 2417241 93 19338786 1287328 284969078 116717514 10144866416 1520589288 0 329180460 1521686240\n";
    Node invert = m.invert(txt);
    output(invert);
  }

  private void writeNode(final JsonGenerator jg, Node invert) throws IOException {
    jg.writeStartObject();
    for (final Map.Entry<String, NodeValue> entry : invert.entrySet()) {
      NodeValue nodeValue = entry.getValue();
      if (nodeValue.isList() && nodeValue.list().size() > 0) {
        jg.writeFieldName(entry.getKey());
        List<Node> list = nodeValue.list();
        boolean array = list.size() > 1;
        if (array) jg.writeStartArray();
        for (Node node : list) {
          writeNode(jg, node);
        }
        if (array) jg.writeEndArray();
      } else {
        String value = nodeValue.value();
        if (value != null) {
          jg.writeFieldName(entry.getKey());
          try {
            double v = Double.parseDouble(value);
            jg.writeNumber(v);
          } catch (NumberFormatException e) {
            jg.writeString(value);
          }
        }
      }
    }
    jg.writeEndObject();
  }
}
