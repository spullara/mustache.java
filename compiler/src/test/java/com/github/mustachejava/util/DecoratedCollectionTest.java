package com.github.mustachejava.util;

import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.Mustache;
import com.github.mustachejava.MustacheFactory;
import com.github.mustachejava.reflect.ReflectionObjectHandler;
import org.junit.Test;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.Collection;

import static org.junit.Assert.assertEquals;

public class DecoratedCollectionTest {
  @Test
  public void testIndexLastFirst() throws IOException {
    MustacheFactory mf = new DefaultMustacheFactory();
    Mustache test = mf.compile(new StringReader(
            "{{#test}}{{index}}: {{#first}}first: {{/first}}{{#last}}last: {{/last}}{{value}}\n{{/test}}"), "test");
    StringWriter sw = new StringWriter();
    test.execute(sw, new Object() {
      Collection test = new DecoratedCollection(Arrays.asList("First", "Second", "Third", "Last"));
    }).flush();
    assertEquals("0: first: First\n1: Second\n2: Third\n3: last: Last\n", sw.toString());
  }

  @Test
  public void testObjectHandler() throws IOException {
    DefaultMustacheFactory mf = new DefaultMustacheFactory();
    mf.setObjectHandler(new ReflectionObjectHandler() {
      @Override
      public Object coerce(Object object) {
        if (object instanceof Collection) {
          return new DecoratedCollection((Collection) object);
        }
        return super.coerce(object);
      }
    });
    Mustache test = mf.compile(new StringReader(
            "{{#test}}{{index}}: {{#first}}first: {{/first}}{{#last}}last: {{/last}}{{value}}\n{{/test}}"), "test");
    StringWriter sw = new StringWriter();
    test.execute(sw, new Object() {
      Collection test = Arrays.asList("First", "Second", "Third", "Last");
    }).flush();
    assertEquals("0: first: First\n1: Second\n2: Third\n3: last: Last\n", sw.toString());
  }

  @Test
  public void testArrayOutput() throws IOException {
    MustacheFactory mf = new DefaultMustacheFactory();
    Mustache test = mf.compile(new StringReader("{{#test}}{{#first}}[{{/first}}{{^first}}, {{/first}}\"{{value}}\"{{#last}}]{{/last}}{{/test}}"), "test");
    StringWriter sw = new StringWriter();
    test.execute(sw, new Object() {
      Collection test = new DecoratedCollection(Arrays.asList("one", "two", "three"));
    }).flush();
    assertEquals("[\"one\", \"two\", \"three\"]", sw.toString());
  }
}
