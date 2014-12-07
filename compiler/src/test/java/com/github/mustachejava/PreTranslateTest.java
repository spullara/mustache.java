package com.github.mustachejava;

import org.junit.Test;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.function.Function;

import static junit.framework.Assert.assertEquals;

/**
 * If you want to precompile translations, you will need to do two passes against
 * templates. This is an example of how to implement this.
 */
public class PreTranslateTest {
  @Test
  public void testPretranslate() throws IOException {
    MustacheFactory mf = new DefaultMustacheFactory() {

      MustacheParser mp = new MustacheParser(this) {
        @Override
        public Mustache compile(Reader reader, String file) {
          return super.compile(reader, file, "{[", "]}");
        }
      };

      @Override
      public Mustache compile(Reader reader, String file, String sm, String em) {
        return super.compile(reader, file, "{[", "]}");
      }

      @Override
      protected Function<String, Mustache> getMustacheCacheFunction() {
        return (template) -> {
          Mustache compile = mp.compile(template);
          compile.init();
          return compile;
        };
      }
    };
    Mustache m = mf.compile("pretranslate.html");
    StringWriter sw = new StringWriter();
    m.execute(sw, new Object() {
      Function i = input -> "{{test}} Translate";
    }).close();
    assertEquals("{{#show}}\n{{test}} Translate\n{{/show}}", sw.toString());
    mf = new DefaultMustacheFactory();
    m = mf.compile(new StringReader(sw.toString()), "pretranslate.html");
    sw = new StringWriter();
    m.execute(sw, new Object() {
      boolean show = true;
      String test = "Now";
    }).close();
    assertEquals("Now Translate\n", sw.toString());
  }
}
