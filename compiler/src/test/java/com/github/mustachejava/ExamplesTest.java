package com.github.mustachejava;

import com.github.mustachejava.reflect.ReflectionObjectHandler;
import com.github.mustachejava.util.GuardException;
import com.github.mustachejava.util.Wrapper;
import org.junit.Test;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class ExamplesTest {
  @Test
  public void testExpressionsInNames() throws IOException {
    DefaultMustacheFactory mf = new DefaultMustacheFactory();
    mf.setObjectHandler(new ReflectionObjectHandler() {
      @Override
      public Wrapper find(String name, List<Object> scopes) {
        // Worst expression parser ever written follows
        String[] split = name.split("[*]");
        if (split.length > 1) {
          final double multiplier = Double.parseDouble(split[1].trim());
          final Wrapper wrapper = super.find(split[0].trim(), scopes);
          return new Wrapper() {
            @Override
            public Object call(List<Object> scopes) throws GuardException {
              Object value = wrapper.call(scopes);
              if (value instanceof Number) {
                value = ((Number) value).doubleValue();
              } else {
                value = value == null ? 0d : Double.parseDouble(value.toString());
              }
              return ((Double) value) * multiplier;
            }
          };
        }
        return super.find(name, scopes);
      }
    });
    Mustache test = mf.compile(new StringReader("{{number * 2.2}}"), "test");
    StringWriter sw = new StringWriter();
    test.execute(sw, new Object() { double number = 10; }).flush();
    assertEquals("22.0", sw.toString());
  }
}
