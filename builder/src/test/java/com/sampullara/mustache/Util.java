package com.sampullara.mustache;

import java.io.IOException;
import java.io.StringWriter;

import org.codehaus.jackson.JsonFactory;
import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.map.MappingJsonFactory;

public class Util {
  public static String toJSON(Scope scope) throws MustacheException {
    StringWriter sw = new StringWriter();
    JsonFactory jf = new MappingJsonFactory();
    try {
      JsonGenerator jg = jf.createJsonGenerator(sw);
      jg.writeObject(scope);
      jg.flush();
    } catch (IOException e) {
      throw new MustacheException(e);
    }
    return sw.toString();
  }
}
