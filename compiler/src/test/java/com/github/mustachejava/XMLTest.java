package com.github.mustachejava;

import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;

public class XMLTest {

  private String xml = "<response>\n" +
          "    <result>\n" +
          "        <invoice>\n" +
          "            <status>2</status>\n" +
          "            <deposit_percentage />\n" +
          "            <create_date>2016-09-09</create_date>\n" +
          "            <outstanding>\n" +
          "                <amount>800.00</amount>\n" +
          "                <code>USD</code>\n" +
          "            </outstanding>\n" +
          "        </invoice>\n" +
          "    </result>\n" +
          "</response>\n";

  private String template = "{{#response}}\n" +
          "{{#result}}\n" +
          "{{#invoice}}\n" +
          "Status: {{status}}\n" +
          "Deposit Percentage: {{deposit_percentage}}\n" +
          "Date: {{create_date}}\n" +
          "{{#outstanding}}\n" +
          "Outstanding: {{amount}} {{code}}\n" +
          "{{/outstanding}}\n" +
          "{{/invoice}}\n" +
          "{{/result}}\n" +
          "{{/response}}";

  private String correct = "Status: 2\n" +
          "Deposit Percentage: \n" +
          "Date: 2016-09-09\n" +
          "Outstanding: 800.00 USD\n";

  private String xml2 = "<entries>\n" +
          "    <source>\n" +
          "        <name>Pictures</name>\n" +
          "            <type>folder</type>\n" +
          "            <created_at>2012-12-12T10:53:43-08:00</created_at>\n" +
          "            <modified_at>2012-12-12T10:53:43-08:00</modified_at>\n" +
          "            <created_by>\n" +
          "                <name>sean rose</name>\n" +
          "                <login>sean@box.com</login>\n" +
          "             </created_by>\n" +
          "                <description />\n" +
          "            <size>0</size>\n" +
          "    </source>\n" +
          "</entries>\n";

  private String template2 = "{{#entries}}\n" +
          "   \"name\" : \"_contentType\", // need to check\n" +
          "   \"value\" : {{source.type}},\n" +
          "   \"name\" : \"_title\",\n" +
          "   \"value\" : \"{{Base_URL}}\"\n" +
          "{{/entries}}";

  private String correct2 = "   \"name\" : \"_contentType\", // need to check\n" +
          "   \"value\" : folder,\n" +
          "   \"name\" : \"_title\",\n" +
          "   \"value\" : \"http://www.google.com\"\n";

  private void put(Node e, Map<String, Object> map) {
    map.put(e.getNodeName(), get(e));
  }

  private Object get(Node de) {
    if (!de.hasChildNodes()) {
      return "";
    } else if (de.getChildNodes().getLength() == 1 && de.getFirstChild() instanceof Text) {
      return de.getTextContent();
    } else {
      NodeList childNodes = de.getChildNodes();
      Map<String, Object> map = new HashMap<>();
      for (int i = 0; i < childNodes.getLength(); i++) {
        Node item = childNodes.item(i);
        if (!(item instanceof Text)) {
          put(item, map);
        }
      }
      return map;
    }
  }

  @Test
  public void testXMLtoMap() throws IOException, SAXException, ParserConfigurationException {
    Map<String, Object> map = convertXMLtoMap(xml);

    DefaultMustacheFactory dmf = new DefaultMustacheFactory();
    Mustache response = dmf.compile(new StringReader(template), "response");
    Writer execute = response.execute(new StringWriter(), map);
    assertEquals(correct, execute.toString());
  }

  @Test
  public void testXMLtoMapWithBase() throws IOException, SAXException, ParserConfigurationException {
    Map<String, Object> map = convertXMLtoMap(xml2);

    DefaultMustacheFactory dmf = new DefaultMustacheFactory();
    Mustache response = dmf.compile(new StringReader(template2), "entries");
    Object base = new Object() {
      public String Base_URL = "http://www.google.com";
    };
    Writer execute = response.execute(new StringWriter(), new Object[] { base, map });
    assertEquals(correct2, execute.toString());
  }

  private Map<String, Object> convertXMLtoMap(String xml2) throws ParserConfigurationException, SAXException, IOException {
    DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
    DocumentBuilder db = dbf.newDocumentBuilder();
    Document doc = db.parse(new ByteArrayInputStream(xml2.getBytes()));
    Node de = doc.getDocumentElement();
    de.normalize();
    Map<String, Object> map = new HashMap<>();
    put(de, map);
    return map;
  }
}
