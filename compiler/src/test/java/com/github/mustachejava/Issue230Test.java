package com.github.mustachejava;

import com.github.mustachejava.codes.ValueCode;
import org.junit.Test;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;

/**
 * Reproduction test case
 */
public class Issue230Test {
  interface NotEncoded {
  }

  @Test
  public void testDotNotationWithNull() throws IOException {
    DefaultMustacheFactory mf = new DefaultMustacheFactory() {
      @Override
      public MustacheVisitor createMustacheVisitor() {
        return new DefaultMustacheVisitor(this) {
          @Override
          public void value(TemplateContext tc, String variable, boolean encoded) {
            list.add(new ValueCode(tc, df, variable, encoded) {
              @Override
              public Writer execute(Writer writer, List<Object> scopes) {
                try {
                  final Object object = get(scopes);
                  if (object instanceof NotEncoded) {
                    writer.write(oh.stringify(object));
                    return appendText(run(writer, scopes));
                  } else {
                    return super.execute(writer, scopes);
                  }
                } catch (Exception e) {
                  throw new MustacheException("Failed to get value for " + name, e, tc);
                }
              }
            });
          }
        };
      }
    };
    Mustache m = mf.compile(new StringReader("{{not_encoded}} {{encoded}}"), "test");
    StringWriter sw = new StringWriter();
    Map<String, Object> map = new HashMap<>();
    map.put("not_encoded", new NotEncoded() {
      @Override
      public String toString() {
        return "<div>";
      }
    });
    map.put("encoded", "<div>");
    m.execute(sw, map).close();
    assertEquals("<div> &lt;div&gt;", sw.toString());
  }
}
