package com.github.mustachejava.inverter;

import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.Mustache;
import com.github.mustachejava.util.Node;
import com.github.mustachejava.util.NodeValue;
import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.map.MappingJsonFactory;
import org.junit.Test;

import java.io.IOException;
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

    MappingJsonFactory jf = new MappingJsonFactory();
    StringWriter out = new StringWriter();
    JsonGenerator jg = jf.createJsonGenerator(out);
    writeNode(jg, invert);
    jg.flush();
    System.out.println(out.toString());
  }

  private void writeNode(final JsonGenerator jg, Node invert) throws IOException {
    jg.writeStartObject();
    for (final Map.Entry<String, NodeValue> entry : invert.entrySet()) {
      jg.writeFieldName(entry.getKey());
      NodeValue nodeValue = entry.getValue();
      if (nodeValue.isList()) {
        List<Node> list = nodeValue.list();
        boolean array = list.size() > 1;
        if (array) jg.writeStartArray();
        for (Node node : list) {
          writeNode(jg, node);
        }
        if (array) jg.writeEndArray();
      } else {
        String value = nodeValue.value();
        try {
          double v = Double.parseDouble(value);
          jg.writeNumber(v);
        } catch (NumberFormatException e) {
          jg.writeString(value);
        }
      }
    }
    jg.writeEndObject();
  }
}
