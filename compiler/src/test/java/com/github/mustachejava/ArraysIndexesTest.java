package com.github.mustachejava;

import com.github.mustachejava.reflect.ReflectionObjectHandler;
import org.junit.Test;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.lang.reflect.Array;
import java.util.AbstractMap;
import java.util.Iterator;
import java.util.Set;

import static org.junit.Assert.assertEquals;

/**
 * Shows a simple way to add indexes for arrays.
 * <p/>
 * User: sam
 * Date: 4/7/13
 * Time: 11:12 AM
 */
public class ArraysIndexesTest {
  @Test
  public void testArrayIndexExtension() throws IOException {
    String template = "<ol>\n" +
            "    <li>{{test.1}}</li>\n" +
            "    <li>{{test.0}}</li>\n" +
            "    <li>{{test.3}}</li>\n" +
            "</ol>\n" +
            "<ol>{{test}}\n" +
            "{{#test}}\n" +
            "    <li>{{.}}</li>\n" +
            "{{/test}}\n" +
            "</ol>";
    String result = "<ol>\n" +
            "    <li>1</li>\n" +
            "    <li>0</li>\n" +
            "    <li>3</li>\n" +
            "</ol>\n" +
            "<ol>\n" +
            "    <li>0</li>\n" +
            "    <li>1</li>\n" +
            "    <li>2</li>\n" +
            "    <li>3</li>\n" +
            "</ol>";
    Object scope = new Object() {
      int[] test = { 0, 1, 2, 3 };
    };
    ReflectionObjectHandler oh = new ReflectionObjectHandler() {
      @Override
      public Object coerce(final Object object) {
        if (object != null && object.getClass().isArray()) {
          return new ArrayMap(object);
        }
        return super.coerce(object);
      }
    };
    DefaultMustacheFactory mf = new DefaultMustacheFactory();
    mf.setObjectHandler(oh);
    Mustache m = mf.compile(new StringReader(template), "template");
    StringWriter writer = new StringWriter();
    m.execute(writer, scope).flush();
    assertEquals(result, writer.toString());
  }

  private static class ArrayMap extends AbstractMap<Object, Object> implements Iterable<Object> {
    private final Object object;

    public ArrayMap(Object object) {
      this.object = object;
    }

    @Override
    public Object get(Object key) {
      try {
        int index = Integer.parseInt(key.toString());
        return Array.get(object, index);
      } catch (NumberFormatException nfe) {
        return null;
      }
    }

    @Override
    public boolean containsKey(Object key) {
      return get(key) != null;
    }

    @Override
    public Set<Entry<Object, Object>> entrySet() {
      throw new UnsupportedOperationException();
    }

    /**
     * Returns an iterator over a set of elements of type T.
     *
     * @return an Iterator.
     */
    @Override
    public Iterator<Object> iterator() {
      return new Iterator<Object>() {

        int index = 0;
        int length = Array.getLength(object);

        @Override
        public boolean hasNext() {
          return index < length;
        }

        @Override
        public Object next() {
          return Array.get(object, index++);
        }

        @Override
        public void remove() {
          throw new UnsupportedOperationException();
        }
      };
    }
  }
}
