package com.github.mustachejava.inverter;

import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.Mustache;
import com.github.mustachejava.Node;
import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.map.MappingJsonFactory;
import org.junit.Test;

import java.io.IOException;
import java.io.StringWriter;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class InvertToJsonTest {
  @Test
  public void testToJson() throws IOException {
    DefaultMustacheFactory dmf = new DefaultMustacheFactory();
    Mustache compile = dmf.compile("fdbcli.mustache");
    Path file = FileSystems.getDefault().getPath("src/test/resources/fdbcli.txt");
    String txt = new String(Files.readAllBytes(file), "UTF-8");
    Node invert = compile.invert(new Node(), txt, new AtomicInteger(0));

    MappingJsonFactory jf = new MappingJsonFactory();
    StringWriter out = new StringWriter();
    JsonGenerator jg = jf.createJsonGenerator(out);
    writeNode(jg, invert);
    jg.flush();
    System.out.println(out.toString());
  }

  private void writeNode(JsonGenerator jg, Node invert) throws IOException {
    String value = invert.value;
    if (value == null) {
      jg.writeStartObject();
      for (Map.Entry<String, List<Node>> entry : invert.entrySet()) {
        List<Node> list = entry.getValue();
        boolean array = list.size() > 1;
        jg.writeFieldName(entry.getKey());
        if (array) jg.writeStartArray();
        for (Node node : list) {
          writeNode(jg, node);
        }
        if (array) jg.writeEndArray();
      }
      jg.writeEndObject();
    } else {
      try {
        double v = Double.parseDouble(value);
        jg.writeNumber(v);
      } catch (NumberFormatException e) {
        jg.writeString(value);
      }
    }
  }
}
